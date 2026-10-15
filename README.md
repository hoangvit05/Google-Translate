# Google Translate Clone (Android App)

Ứng dụng mô phỏng Google Translate phát triển trên nền tảng **Android (Kotlin)**. Dự án sử dụng API dịch thuật trực tiếp (MyMemory API) mà không cần thiết lập server trung gian, giúp dễ dàng cài đặt và chạy trên mọi thiết bị.

## 👥 Thành viên nhóm & Đồ án
* **Môn học:** Lập trình di động / Đồ án nhóm
* **Nền tảng:** Android (Kotlin), XML Layout, RESTful API (MyMemory API), Kotlin Coroutines, ViewBinding

---

## 🚀 Các tính năng chính
1. **Dịch văn bản đa ngôn ngữ:** Hỗ trợ dịch giữa Tiếng Anh, Tiếng Việt, Tiếng Pháp, Tiếng Nhật, Tiếng Hàn và Tiếng Trung trực tiếp qua MyMemory API miễn phí.
2. **Đổi chiều ngôn ngữ thông minh (Swap):** Đảo ngược ngôn ngữ nguồn, ngôn ngữ đích và tự động hoán đổi nội dung văn bản tức thì.
3. **Sao chép văn bản (Copy to Clipboard):** Sao chép kết quả dịch sang bộ nhớ tạm nhanh chóng chỉ với một chạm.
4. **Xóa nhanh văn bản (Clear text):** Nút xóa nhanh thông minh tự động hiển thị khi người dùng nhập văn bản để dọn sạch ô nhập liệu.
5. **Tối ưu hiệu năng & Tránh xung đột:** Tự động hủy yêu cầu dịch cũ (`Job Cancel`) khi người dùng bấm dịch liên tục, đồng thời tự động giải mã các ký tự HTML Entity chuẩn xác.

---

## 🛠️ Cấu trúc dự án
* `app/src/main/java/com/example/googletranslateclone/MainActivity.kt`: Xử lý logic giao diện, gọi API dịch thuật và quản lý luồng bất đồng bộ với Kotlin Coroutines (`lifecycleScope`).
* `app/src/main/res/layout/activity_main.xml`: Giao diện XML chuẩn Material Design kết hợp hệ thống Vector Drawables (`ic_swap`, `ic_clear`, `ic_copy`).
* `app/src/main/AndroidManifest.xml`: Cấu hình quyền `INTERNET` và khởi chạy ứng dụng.

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
