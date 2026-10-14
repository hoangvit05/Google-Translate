# Google Translate Clone (Android App)

Ứng dụng mô phỏng Google Translate phát triển trên nền tảng **Android (Kotlin)**. Dự án sử dụng API dịch thuật trực tiếp (MyMemory API) mà không cần thiết lập server trung gian, giúp dễ dàng cài đặt và chạy trên mọi thiết bị.

## 👥 Thành viên nhóm & Đồ án
* **Môn học:** Lập trình di động / Đồ án nhóm
* **Nền tảng:** Android (Kotlin), XML Layout, RESTful API (MyMemory API)

---

## 🚀 Các tính năng chính
1. **Dịch văn bản đa ngôn ngữ:** Hỗ trợ dịch giữa Tiếng Anh, Tiếng Việt, Tiếng Pháp, Tiếng Nhật, Tiếng Hàn và Tiếng Trung. Gọi trực tiếp từ MyMemory API miễn phí.
2. **Nhận diện giọng nói (Speech-to-Text):** Hỗ trợ thu âm giọng nói để chuyển thành văn bản cần dịch.
3. **Phát âm văn bản (Text-to-Speech):** Đọc to văn bản gốc và kết quả dịch bằng giọng nói chuẩn bản xứ.
4. **Đổi chiều ngôn ngữ nhanh (Swap):** Đảo ngược ngôn ngữ nguồn và ngôn ngữ đích tức thì.

---

## 🛠️ Cấu trúc dự án
* `app/src/main/java/com/example/googletranslateclone/MainActivity.kt`: Xử lý logic giao diện, gọi API dịch thuật và tương tác TTS/Speech.
* `app/src/main/res/layout/activity_main.xml`: Giao diện XML của ứng dụng.
* `app/src/main/AndroidManifest.xml`: Cấu hình quyền `INTERNET`, `RECORD_AUDIO` và cấu hình ứng dụng.

---

## ⚙️ Hướng dẫn cài đặt và chạy dự án

1. Clone mã nguồn từ GitHub về máy:
   ```bash
   git clone https://github.com/hoangvit05/Google-Translate.git
   ```
2. Mở thư mục dự án vừa tải về (`Google-Translate`) bằng phần mềm **Android Studio**.
3. Đợi Android Studio đồng bộ Gradle (`Sync Project with Gradle Files`).
4. Khởi động máy ảo Android Emulator hoặc kết nối thiết bị thật (yêu cầu có Internet).
5. Bấm nút **Run (▶)** (hoặc Shift+F10) để cài đặt và chạy ứng dụng.
