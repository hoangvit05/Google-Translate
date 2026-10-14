# Google Translate Clone (Android & Node.js)

Ứng dụng mô phỏng Google Translate phát triển trên nền tảng **Android (Kotlin)** kết hợp với **Node.js (Backend)** dùng để dịch thuật ngôn ngữ đa quốc gia.

## 👥 Thành viên nhóm & Đồ án
* **Môn học:** Lập trình di động / Đồ án nhóm
* **Nền tảng:** Android (Kotlin), XML Layout, Node.js (HTTP Server / Fetch API)

---

## 🚀 Các tính năng chính
1. **Dịch văn bản đa ngôn ngữ:** Hỗ trợ dịch giữa Tiếng Anh, Tiếng Việt, Tiếng Pháp, Tiếng Nhật, Tiếng Hàn và Tiếng Trung thông qua Server Node.js trung gian.
2. **Nhận diện giọng nói (Speech-to-Text):** Hỗ trợ thu âm giọng nói để chuyển thành văn bản cần dịch.
3. **Phát âm văn bản (Text-to-Speech):** Đọc to văn bản gốc và kết quả dịch bằng giọng nói chuẩn bản xứ.
4. **Đổi chiều ngôn ngữ nhanh (Swap):** Đảo ngược ngôn ngữ nguồn và ngôn ngữ đích tức thì.

---

## 🛠️ Cấu trúc dự án
* `app/src/main/java/com/example/googletranslateclone/MainActivity.kt`: Xử lý logic giao diện, gọi API dịch thuật và tương tác TTS/Speech.
* `app/src/main/res/layout/activity_main.xml`: Giao diện XML của ứng dụng.
* `app/src/main/AndroidManifest.xml`: Cấu hình quyền `INTERNET`, `RECORD_AUDIO` và cấu hình ứng dụng.
* `server.js` (Backend): Server Node.js nhận request từ app Android và gọi API dịch thuật công khai.

---

## ⚙️ Hướng dẫn cài đặt và chạy dự án

### 1. Chạy Backend (Node.js Server)
1. Tạo một thư mục mới (ví dụ `server`), tạo file `server.js` với nội dung server Node.js.
2. Mở Terminal tại thư mục đó và chạy lệnh:
   ```bash
   node server.js
   ```
3. Server sẽ chạy tại cổng `5000` (`http://localhost:5000/api/translate`).

### 2. Chạy ứng dụng Android (Android Studio)
1. Mở dự án `GoogleTranslateClone` trong Android Studio.
2. Đồng bộ Gradle (`Sync Project with Gradle Files`).
3. Khởi động máy ảo Android Emulator (Ví dụ: *Medium Phone API 37.0*).
4. Bấm nút **Run (▶)** để cài đặt và chạy ứng dụng trên máy ảo.
