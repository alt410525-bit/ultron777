# Ultron tool status

Registry: `core/tools/` (Tool, SpecTool, ToolRegistry). Naya tool = module ki list mein `SpecTool(...) { args, ctx -> ... }`.
Module list: `tools/ToolCatalog.kt`.

## Abhi registered (51 naye + 23 purane)

### Utility (`tools/utility`, 20)
util_calculator, util_unit_convert, util_bmi, util_tip, util_emi, util_date_diff, util_countdown,
util_roman, util_number_base, util_random, util_password_strength, util_morse, util_caesar_cipher,
util_text_transform, util_color_convert, util_percent, util_water_intake, util_zodiac,
util_world_time, util_time_now

### Developer (`tools/developer`, 10)
dev_base64_encode, dev_base64_decode, dev_url_encode, dev_url_decode, dev_hash, dev_json_format,
dev_json_validate, dev_uuid, dev_regex_test, dev_text_diff

### Device & hardware (`tools/device`, 21)
device_flashlight_set, device_volume_status, device_volume_set, device_audio_mode_set,
device_brightness_status, device_brightness_set, device_rotation_set, device_vibrate,
device_clipboard_read, device_clipboard_write, device_open_settings, device_network_status,
device_dnd_status, device_dnd_set, device_app_launch, device_installed_apps, device_info,
device_memory_usage, device_sensors_list, device_sim_info, device_location

### AI, persona & memory (`tools/ai`, 23) — Batch 2
ai_memory_save, ai_memory_search, ai_memory_list, ai_memory_delete, ai_memory_clear, ai_memory_stats,
ai_persona_get, ai_persona_set, ai_prompt_template_list, ai_prompt_template_render,
ai_token_estimate, ai_summarize_text, ai_sentiment, ai_intent_classify, ai_context_snapshot,
ai_alias_add, ai_alias_list, ai_alias_remove, ai_todo_add, ai_todo_list, ai_todo_done,
ai_backup_create, ai_backup_restore

Note: 18+ / intense mode ka tool jaan-bujhkar nahi banaya. Ye existing adult-confirmation gate ke peeche hi rahega.

## Abhi nahi bana (aur kyun)

- **Wi-Fi / Bluetooth / Hotspot / Airplane toggle**: Android 10+ par normal apps ko ye toggle karne ki permission nahi milti. `device_open_settings` panel khol deta hai.
- **Package install/uninstall, reboot/shutdown, default launcher, system language, date/time sync**: system/privileged app chahiye.
- **Screenshot, screen recording, MediaProjection**: user consent aur foreground service flow chahiye, alag batch.
- **WhatsApp auto-reply, notification controller, call recording**: NotificationListenerService aur legal/consent check chahiye. Call recording ke liye India mein consent rules dekhne honge.
- **Telegram receiver, Gmail/SMTP, Notion/Termux/APK bridge, Git automation**: network/API keys aur external app integration chahiye.
- **Media controls, camera capture, QR scanner, OCR, barcode**: CameraX/ML Kit dependency chahiye.
- **Baaki list** (AI persona/memory, prompt tools, backup/restore, games, sensors live reading, weather/news/crypto/stock fetchers): next batches.

## Notes
- Compile aur runtime par test nahi kiya gaya hai (Android SDK sandbox mein nahi tha). GitHub Actions build se verify karo.
- `SpecTool` check karta hai ki runtime permissions di gayi hain ya nahi. Special permissions (WRITE_SETTINGS, notification policy) tool ke andar check hoti hain.

## Batch 3 (naye 20 tools)

Files: `tools/connectors/ConnectorsBatch3.kt`, `tools/developer/DeveloperBatch3.kt`, `tools/utility/UtilityBatch3.kt`, `tools/device/DeviceBatch3.kt`, `tools/messaging/MessagingBatch3.kt`. Modules: `tools/Batch3Modules.kt`, ToolCatalog mein register.

- GitHub: `github_commit_history`, `github_comment_issue` (HIGH risk, confirm chahiye), `github_build_failures`
- Developer: `dev_code_clean`, `dev_webhook_test` (HIGH, sirf https), `dev_port_status` (localhost), `dev_note_save`, `dev_note_read`, `dev_json_to_kotlin`
- Utility: `util_text_encrypt`, `util_text_decrypt` (AES-256-GCM, PBKDF2), `util_ping` (TCP connect time)
- Device: `device_set_alarm`, `device_set_timer`, `device_share_text`, `device_calendar_add`, `device_sensor_read` (light, compass, accelerometer)
- Messaging: `messaging_sms_search` (READ_SMS), `messaging_contact_search` (READ_CONTACTS), `messaging_contact_add` (Contacts app mein form)

### Verify nahi hua
- Compile aur runtime test nahi hua (Android SDK aur Maven is sandbox mein nahi hain). Sirf brace/paren balance check hua.
- GitHub Actions build se pehle verify karo. Build fail hone par error pehle `Batch3*` files mein dekho.
- `util_ping` aur `dev_webhook_test` ka INTERNET permission manifest mein hai.

### Jaan-bujhkar nahi banaye
- Doosron ki chats, payments ya call recording padhne wale tools (privacy). Recent Chats Viewer, Chat Analytics, P2P payment reader, WhatsApp status downloader, call recording.
- 18+ partner mode ka koi naya tool.
- Wifi/Bluetooth hacking, jailbreak, malware, undetectable apps.
- Reverse-engineering / APK decompile tool.

### Abhi bhi baaki
- Wake word detector (`WakeWordService.isWakePhraseDetected` abhi `false` return karta hai). Porcupine ya ONNX model lagana hoga.
- Gemini model name (`gemini-1.5-flash`) purana ho sakta hai, check karo.
- Baaki Planned (P) items: `ultron_gap_report.md` mein list hai.
