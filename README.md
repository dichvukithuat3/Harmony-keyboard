# Bàn phím Hà — Bàn phím Tiếng Việt cho Android / HarmonyOS 4.x

Bàn phím gõ tiếng Việt **Telex và VNI**, viết riêng, không phụ thuộc thư viện bên ngoài.

> **Lưu ý quan trọng về phiên bản HarmonyOS**
>
> - **HarmonyOS 4.x và cũ hơn** chạy trên nền Android → cài được file `.apk` này.
> - **HarmonyOS NEXT (5.0 / 6.0 / 7.0)** đã bỏ hoàn toàn khả năng chạy ứng dụng Android → **không cài được `.apk`**, cần bản `.hap` riêng (làm ở nhánh khác của dự án).

## Tải và cài đặt

1. Vào tab **Releases** của repo này.
2. Tải file `BanPhimHa-vX.X.X.apk` về điện thoại.
3. Mở file vừa tải để cài (nếu máy hỏi, cho phép "Cài đặt từ nguồn không xác định").
4. Mở app **Bàn phím Hà** và làm theo 3 bước trên màn hình:
   - Bước 1: bật bàn phím trong Cài đặt hệ thống
   - Bước 2: chọn làm bàn phím đang dùng
   - Bước 3: gõ thử

## Cách gõ

| | Telex | VNI |
|---|---|---|
| sắc | `s` | `1` |
| huyền | `f` | `2` |
| hỏi | `r` | `3` |
| ngã | `x` | `4` |
| nặng | `j` | `5` |
| xóa dấu | `z` | `0` |
| â ê ô | `aa` `ee` `oo` | `a6` `e6` `o6` |
| ă | `aw` | `a8` |
| ơ ư | `ow` `uw` | `o7` `u7` |
| đ | `dd` | `d9` |

Ví dụ: `vieejt` → **việt**, `dduowcj` → **được**, `nguyeenx` → **nguyễn**, `xoaif` → **xoài**.

Gõ lặp lại phím dấu để bỏ dấu: `ass` → `as`, `aaa` → `aa`.

## Tính năng

- Telex + VNI, chuyển đổi ngay trên bàn phím hoặc trong Cài đặt
- Đặt dấu đúng chuẩn chính tả (hòa, thủy, khỏe, được, người, quý, già)
- Nút chuyển nhanh VI / EN
- Bàn phím chữ, số, ký hiệu; Shift và Caps Lock (bấm Shift hai lần)
- Giữ phím để chọn ký tự phụ (giữ `a` → â ă, giữ `o` → ô ơ, giữ `d` → đ)
- Giữ phím xóa để xóa liên tục
- Tùy chỉnh: rung, âm thanh, ký tự phóng to khi bấm, hàng số, chiều cao bàn phím

## Tự build

Không cần build thủ công — mỗi lần đẩy code lên nhánh `main`, GitHub Actions tự build và đăng file APK vào mục **Releases**.

Muốn build trên máy mình thì cần Android SDK và chạy:

```bash
gradle :app:assembleRelease
```

## Kiểm thử

Engine tiếng Việt có bộ test chạy độc lập, không cần Android:

```bash
mkdir -p build/enginetest
javac -encoding UTF-8 -d build/enginetest \
  app/src/main/java/com/hakeyboard/core/VietnameseEngine.java tools/EngineTest.java
java -cp build/enginetest EngineTest
```

Bộ test này cũng chạy tự động trong CI; nếu sai một ca nào thì build dừng, không ra APK.

## Về chữ ký ứng dụng

File `keystore/hakeyboard.jks` nằm sẵn trong repo để mọi bản build đều ký cùng một khóa,
nhờ đó bản mới cài đè lên bản cũ được mà không phải gỡ đi.

Đây là khóa dùng riêng cho ứng dụng cá nhân này. Nếu repo để **công khai**, bất kỳ ai cũng
đọc được khóa đó — nên để repo ở chế độ **private** nếu bạn muốn chặt chẽ hơn.
