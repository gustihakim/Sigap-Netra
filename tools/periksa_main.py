"""
periksa_main.py - pemeriksa statis untuk main.py
==================================================
JALANKAN DI PC, BUKAN DI MAIXCAM.

    python3 periksa_main.py                 <- cari main.py sendiri
    python3 periksa_main.py path/ke/main.py

Berkas ini TIDAK mengimpor maix dan tidak menyentuh perangkat sama
sekali. Gunanya justru memeriksa SEBELUM diunggah - kalau dijalankan di
MaixCam lewat MaixVision, ia sudah terlambat menolong.

main.py sendiri tidak bisa dijalankan di PC (butuh modul maix), jadi
kesalahannya baru ketahuan di perangkat: satu putaran unggah, jalan, baca
log, setiap kali. Pemeriksa ini memangkas putaran itu untuk kelas
kesalahan yang paling sering lolos.

TIGA KESALAHAN NYATA YANG SUDAH TERJADI, DAN KENAPA PEMERIKSA LAMA
GAGAL MENANGKAPNYA

    1. self.cam dibaca di daftar laporan yang dibangun SEBELUM kamera
       dibuat.
       Pemeriksa lama cuma bertanya "pernah di-set di suatu tempat?" -
       jawabannya ya, jadi lolos. Yang benar: apakah di-set SEBELUM
       dibaca, mengikuti urutan __init__.

    2. tampilkan() memanggil dirinya sendiri, akibat penggantian massal
       yang ikut mengganti baris di dalam fungsinya sendiri. Rekursi tak
       berujung.

    3. self.kanvas_kamera dibaca kanvas() tapi hanya di-set di dalam
       tampilkan(). Pemeriksa lama lolos karena atributnya MEMANG ada
       assignment-nya - di metode lain yang belum tentu pernah jalan.

    Ketiganya sekelas: "ada di suatu tempat" tidak sama dengan "ada saat
    dibutuhkan". Pemeriksa ini menanyakan yang kedua.
"""

import ast
import os
import sys


def periksa(jalur):
    src = open(jalur, encoding="utf-8").read()
    tree = ast.parse(src)
    masalah = []

    print("=" * 62)
    print("  periksa_main.py  ->  %s" % jalur)
    print("=" * 62)
    print("%d baris | CRLF %s | non-ascii %s"
          % (len(src.splitlines()), "\r\n" in src,
             sorted({c for c in src if ord(c) > 127}) or "tidak ada"))

    for kelas in [n for n in tree.body if isinstance(n, ast.ClassDef)]:
        metode = {f.name: f for f in kelas.body
                  if isinstance(f, ast.FunctionDef)}
        kvar = {t.id for a in kelas.body if isinstance(a, ast.Assign)
                for t in a.targets if isinstance(t, ast.Name)}

        # --- 1. rekursi langsung
        for nama, f in metode.items():
            for n in ast.walk(f):
                if (isinstance(n, ast.Call)
                        and isinstance(n.func, ast.Attribute)
                        and isinstance(n.func.value, ast.Name)
                        and n.func.value.id == "self"
                        and n.func.attr == nama):
                    masalah.append("%s.%s() memanggil DIRINYA SENDIRI "
                                   "(baris %d)" % (kelas.name, nama, n.lineno))

        # --- 2. atribut di-set di mana saja?
        diset, dibaca = set(), {}
        for n in ast.walk(kelas):
            if (isinstance(n, ast.Attribute)
                    and isinstance(n.value, ast.Name)
                    and n.value.id == "self"):
                if isinstance(n.ctx, ast.Store):
                    diset.add(n.attr)
                else:
                    dibaca.setdefault(n.attr, n.lineno)
        for a, baris in sorted(dibaca.items()):
            if a not in diset and a not in metode and a not in kvar:
                masalah.append("%s.self.%s dibaca tapi TIDAK PERNAH di-set "
                               "(baris %d)" % (kelas.name, a, baris))

        # --- 3. di-set HANYA di metode lain, bukan __init__
        init = metode.get("__init__")
        if init:
            di_init = {n.attr for n in ast.walk(init)
                       if isinstance(n, ast.Attribute)
                       and isinstance(n.value, ast.Name)
                       and n.value.id == "self"
                       and isinstance(n.ctx, ast.Store)}
            for nama, f in metode.items():
                if nama == "__init__":
                    continue
                for n in ast.walk(f):
                    if (isinstance(n, ast.Attribute)
                            and isinstance(n.value, ast.Name)
                            and n.value.id == "self"
                            and isinstance(n.ctx, ast.Load)
                            and n.attr in diset
                            and n.attr not in di_init
                            and n.attr not in metode
                            and n.attr not in kvar):
                        masalah.append(
                            "%s.self.%s dibaca di %s() tapi TIDAK diberi "
                            "nilai awal di __init__ (baris %d)"
                            % (kelas.name, n.attr, nama, n.lineno))

            # --- 4. dibaca sebelum di-set, mengikuti urutan __init__
            punya, urut = set(), []

            def jalan(node):
                for n in ast.iter_child_nodes(node):
                    if (isinstance(n, ast.Attribute)
                            and isinstance(n.value, ast.Name)
                            and n.value.id == "self"):
                        if (isinstance(n.ctx, ast.Load)
                                and n.attr not in punya
                                and n.attr not in metode
                                and n.attr not in kvar):
                            urut.append((n.attr, n.lineno))
                        elif isinstance(n.ctx, ast.Store):
                            punya.add(n.attr)
                    jalan(n)

            for s in init.body:
                jalan(s)
            for a, baris in urut:
                masalah.append("%s.self.%s dibaca di __init__ SEBELUM "
                               "di-set (baris %d)" % (kelas.name, a, baris))

    # --- 5. nama global yang tidak dikenal (salah ketik)
    import builtins
    ada = set(dir(builtins))
    for n in ast.walk(tree):
        if isinstance(n, (ast.FunctionDef, ast.ClassDef)):
            ada.add(n.name)
        elif isinstance(n, ast.Name) and isinstance(n.ctx, ast.Store):
            ada.add(n.id)
        elif isinstance(n, ast.arg):
            ada.add(n.arg)
        elif isinstance(n, (ast.Import, ast.ImportFrom)):
            for a in n.names:
                ada.add((a.asname or a.name).split(".")[0])
        elif isinstance(n, ast.ExceptHandler) and n.name:
            ada.add(n.name)
        elif isinstance(n, (ast.Global, ast.Nonlocal)):
            ada.update(n.names)
    hilang = sorted({n.id for n in ast.walk(tree)
                     if isinstance(n, ast.Name)
                     and isinstance(n.ctx, ast.Load)} - ada - {"__file__"})
    for h in hilang:
        masalah.append("nama '%s' dipakai tapi tidak dikenal" % h)

    print()
    if masalah:
        for m in masalah:
            print("  MASALAH  " + m)
        print("\n  %d masalah ditemukan" % len(masalah))
    else:
        print("  bersih - tidak ada masalah ditemukan")
    print("=" * 62)
    return len(masalah)


def cari_main():
    """
    Cari main.py sendiri.

    Versi pertama memakai jalur tetap 'OCR/main.py' dan langsung gagal
    dengan FileNotFoundError begitu dijalankan dari folder lain. Sekarang
    dicari di tempat-tempat yang masuk akal, dimulai dari folder tempat
    berkas ini berada - biasanya main.py memang tetangganya.
    """
    di_sini = os.path.dirname(os.path.abspath(__file__))
    calon = [os.path.join(di_sini, "main.py"),
             os.path.join(di_sini, "sigap_netra", "main.py"),
             os.path.join(os.getcwd(), "main.py"),
             os.path.join(os.getcwd(), "sigap_netra", "main.py"),
             os.path.join(os.getcwd(), "OCR", "main.py")]
    for c in calon:
        if os.path.exists(c) and os.path.abspath(c) != os.path.abspath(__file__):
            return c
    return None


if __name__ == "__main__":
    if len(sys.argv) > 1:
        jalur = sys.argv[1]
        if not os.path.exists(jalur):
            print("Tidak ada berkas: %s" % jalur)
            sys.exit(2)
    else:
        jalur = cari_main()
        if jalur is None:
            print("main.py tidak ditemukan. Sebutkan jalurnya:")
            print("    python3 periksa_main.py path/ke/main.py")
            print("\nDicari di:")
            print("    <folder berkas ini>/main.py")
            print("    <folder berkas ini>/sigap_netra/main.py")
            print("    <folder sekarang>/main.py")
            sys.exit(2)
    sys.exit(1 if periksa(jalur) else 0)