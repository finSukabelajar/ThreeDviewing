# ThreeDViewing — Minggu 11: 3D Visual Realism

Lanjutan proyek Minggu 10 (kamera orbital, proyeksi, stereo anaglyph). Kini memakai
**OpenGL 3.3 core + GLSL**, dengan Phong/Blinn-Phong, Flat/Smooth/Gouraud, HSR,
tekstur, dan shadow mapping.

## Menjalankan
Prasyarat: JDK 17+, Maven, GPU yang mendukung OpenGL 3.3. `pom.xml` memakai natives **Windows**.

    mvn compile exec:java

(macOS: tambahkan `-XstartOnFirstThread` dan ganti classifier natives; Linux: `natives-linux`.)

## Kontrol
| Tombol | Fungsi |
|---|---|
| Mouse drag / scroll | Orbit / zoom |
| 1–6, `[` `]`, S, `+` `-` | Proyeksi, FOV, stereo anaglyph, IPD (dari Minggu 10) |
| **B** | Phong <-> Blinn-Phong |
| N | Eksponen Blinn x4 (setara Phong) <-> x1 (eksponen sama persis) |
| **F** | Flat <-> Smooth (normal rata-rata per vertex) |
| G | Gouraud (per vertex) <-> Phong shading (per pixel) |
| M | Wireframe |
| **Z** | Z-buffer on/off |
| **C** | Back-face culling on/off |
| Y | Demo z-fighting: off -> bug -> fixed (polygon offset) |
| T | Filter tekstur: Nearest -> Bilinear -> Bilinear+Mipmap |
| X | Tekstur on/off |
| **H** | Bayangan on/off |
| P | PCF on/off |
| `,` `.` | Bias bayangan lebih kecil / lebih besar (x0.5 / x2) |
| L | Animasi cahaya |

Judul jendela menampilkan FPS dan seluruh status aktif.

## Struktur
    com.grafkom
    ├── ThreeDViewing              window, loop, input, FPS
    ├── camera/                    Camera3D, OrbitalController          (Minggu 10)
    ├── projection/                PerspectiveProjection, ProjectionTaxonomyManager (Minggu 10)
    └── render/
        ├── SceneRenderer          shadow pass -> main pass
        ├── Scene, SceneObject     lantai, bola, kubus, torus, penanda cahaya, kuadrat z-fight
        ├── MeshFactory, MeshData  geometri + normal flat/smooth (dihitung di CPU)
        ├── Mesh                   VAO/VBO/EBO
        ├── ShaderProgram          compile/link/uniform
        ├── Texture2D              STB + filter Nearest/Bilinear/Mipmap
        ├── ShadowMapFBO           depth FBO 2048x2048
        ├── HSRManager             depth test + culling
        ├── Light                  cahaya directional + lightSpaceMatrix (ortho)
        ├── RenderSettings         semua toggle
        └── StereoAnaglyphRenderer (di-refactor: callback menerima matriks proyeksi & view)
    resources/shaders: phong.*, gouraud.*, depth.*
    resources/textures: uvgrid.png, brick.png, checker.png  (ganti dengan gambar sendiri bila mau)

## Catatan desain (berguna untuk laporan)
- **Normal smooth** = jumlah normal face dibobot sudut interior, dilas per POSISI
  (seam UV dan kutub bola ikut mendapat normal sama). Tanpa bobot, vertex di diagonal sisi
  dihitung dua kali sehingga normal sudut kubus tidak simetris.
- **Eksponen Blinn-Phong** ~4x eksponen Phong agar highlight sebanding (sudut N·H = setengah sudut R·V).
- **Bayangan** hanya mengurangi diffuse+specular, bukan ambient. Bias = max(biasMax*(1-N·L), 0.16*biasMax)
  dalam satuan kedalaman [0,1] (rentang kedalaman cahaya = 28 unit dunia).
- Shadow pass selalu memakai depth test ON, walau Z-buffer utama dimatikan dengan tombol Z.
- UV memakai v=0 di bawah; gambar di-flip saat dimuat. `uvgrid.png` bertuliskan "F"/TOP/BOTTOM
  untuk memastikan arah V benar.
