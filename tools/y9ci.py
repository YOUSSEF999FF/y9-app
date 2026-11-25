#!/usr/bin/env python3
"""
Y9 PRO V5 - runs INSIDE GitHub Actions (nothing to run on your phone).

Inputs (GitHub Secrets / env):
  Y9_MASTER_KEY    long secret phrase (24+ chars). ALL keys are derived from it, so nothing has to be stored:
                   config AES key, config signing key, APK signing key. Keep a private copy of it!
  Y9_APP_PASSWORD  the password users must type in the app (8+ chars)
  Y9_REPO          "user/repo"   (set by the workflow)      Y9_BRANCH  default branch (set by the workflow)

Reads   config/config.json   (public: labels + links only, NO password in it)
Writes  site/config.enc.json (encrypted + signed, committed)   site/release.crt (public certificate)
        keys.properties      (used by Gradle in the same job; git-ignored, never committed)
"""
import base64, datetime, hashlib, json, os, sys, time

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

PBKDF2_ITERATIONS = 120000
P256_ORDER = 0xFFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551


def die(msg):
    print(f"::error::{msg}")
    sys.exit(1)


def b64(b):
    return base64.b64encode(b).decode()


def derive_all(master: str):
    okm = Scrypt(salt=b"y9v5-master-v1", length=32, n=2 ** 16, r=8, p=1).derive(master.encode("utf-8"))

    def sub(label, n):
        return HKDF(algorithm=hashes.SHA256(), length=n, salt=None, info=label.encode()).derive(okm)

    def ec_key(label):
        d = int.from_bytes(sub(label, 32), "big") % (P256_ORDER - 1) + 1
        return ec.derive_private_key(d, ec.SECP256R1())

    return {
        "aes": sub("aes-config", 32),
        "sign": ec_key("sign-config"),
        "apk": ec_key("apk-signing"),
        "pw_salt": sub("password-salt", 16),
        "ks_pass": base64.urlsafe_b64encode(sub("keystore-pass", 18)).decode(),
    }


def spki(pub):
    return pub.public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)


def apk_cert(key, crt_path):
    """Same key + same cert every time (Android only allows in-place updates if the cert never changes)."""
    if os.path.exists(crt_path):
        c = x509.load_pem_x509_certificate(open(crt_path, "rb").read())
        if spki(c.public_key()) == spki(key.public_key()):
            return c
        print("Note: old release.crt does not match the current master key; creating a new one.")
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "Y9 PRO V5")])
    b = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
         .serial_number(1)
         .not_valid_before(datetime.datetime(2025, 1, 1)).not_valid_after(datetime.datetime(2055, 1, 1)))
    try:
        cert = b.sign(key, hashes.SHA256(), ecdsa_deterministic=True)
    except TypeError:  # older library: the cert gets saved below and reused from then on
        cert = b.sign(key, hashes.SHA256())
    os.makedirs(os.path.dirname(crt_path), exist_ok=True)
    open(crt_path, "wb").write(cert.public_bytes(serialization.Encoding.PEM))
    return cert


def main():
    master = os.environ.get("Y9_MASTER_KEY", "")
    app_pw = os.environ.get("Y9_APP_PASSWORD", "")
    repo = os.environ.get("Y9_REPO", "")
    branch = os.environ.get("Y9_BRANCH", "main")
    if len(master) < 24:
        die("Secret Y9_MASTER_KEY is missing or shorter than 24 characters.")
    if len(app_pw) < 8:
        die("Secret Y9_APP_PASSWORD is missing or shorter than 8 characters.")
    if "/" not in repo:
        die("Y9_REPO must look like user/repo.")

    k = derive_all(master)

    # ---- config: inject the password hash (deterministic salt => same password = same hash) ----
    with open("config/config.json", encoding="utf-8") as f:
        cfg = json.load(f)
    h = hashlib.pbkdf2_hmac("sha256", app_pw.encode("utf-8"), k["pw_salt"], PBKDF2_ITERATIONS, 32)
    cfg["password"] = {"salt": b64(k["pw_salt"]), "iter": PBKDF2_ITERATIONS, "hash": b64(h)}
    plain = json.dumps(cfg, ensure_ascii=False, separators=(",", ":")).encode("utf-8")

    seq = int(time.time())
    iv = os.urandom(12)
    ct = AESGCM(k["aes"]).encrypt(iv, plain, None)
    iv_b, ct_b = b64(iv), b64(ct)
    sig = k["sign"].sign(f"y9v5.{seq}.{iv_b}.{ct_b}".encode(), ec.ECDSA(hashes.SHA256()))
    os.makedirs("site", exist_ok=True)
    with open("site/config.enc.json", "w") as f:
        json.dump({"v": 1, "seq": seq, "iv": iv_b, "ct": ct_b, "sig": b64(sig)}, f, separators=(",", ":"))

    # ---- APK signing keystore + build properties for Gradle ----
    cert = apk_cert(k["apk"], "site/release.crt")
    enc = (serialization.PrivateFormat.PKCS12.encryption_builder()
           .kdf_rounds(50000).key_cert_algorithm(pkcs12.PBES.PBESv1SHA1And3KeyTripleDESCBC)
           .hmac_hash(hashes.SHA1()).build(k["ks_pass"].encode()))
    p12 = pkcs12.serialize_key_and_certificates(b"y9", k["apk"], cert, None, enc)
    lines = [
        f"config_url=https://raw.githubusercontent.com/{repo}/{branch}/site/config.enc.json",
        f"sign_public_key={b64(spki(k['sign'].public_key()))}",
        f"aes_key={b64(k['aes'])}",
        f"keystore_b64={b64(p12)}",
        f"keystore_password={k['ks_pass']}",
    ]
    with open("keys.properties", "w") as f:
        f.write("\n".join(lines) + "\n")
    print(f"OK: config built (seq={seq}), keys derived, keystore ready. Nothing secret was printed or stored.")


if __name__ == "__main__":
    main()
