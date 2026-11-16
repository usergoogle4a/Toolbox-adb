package com.example.data

enum class AdbCommandCategory {
    CONNECTION,
    PACKAGES,
    SHELL_SYSTEM,
    ACTIVITY_MANAGER,
    DISPLAY_SETTINGS,
    FILES_SYNC,
    LOGS_DUMPSYS,
    INPUT_CONTROL,
    POWER_BOOTLOADER
}

data class AdbCommandSpec(
    val id: String,
    val category: AdbCommandCategory,
    val titleEn: String,
    val titleRu: String,
    val descriptionEn: String,
    val descriptionRu: String,
    val fullAdbSyntax: String,
    val shellCommand: String?, // non-null if executable directly inside active ADB shell
    val specialAction: String? = null // e.g., "tcpip", "reboot", "reboot_bootloader", "reboot_recovery", "screencap"
)

object AdbCommandCatalog {
    val all: List<AdbCommandSpec> = listOf(
        // CONNECTION & SERVER
        AdbCommandSpec(
            id = "conn_devices",
            category = AdbCommandCategory.CONNECTION,
            titleEn = "List connected devices",
            titleRu = "Список подключённых устройств",
            descriptionEn = "Shows all attached emulators and devices with serials and transport state.",
            descriptionRu = "Показывает все подключённые устройства, серийники и статус отладки.",
            fullAdbSyntax = "adb devices -l",
            shellCommand = null
        ),
        AdbCommandSpec(
            id = "conn_tcpip",
            category = AdbCommandCategory.CONNECTION,
            titleEn = "Enable ADB over Wi-Fi (5555)",
            titleRu = "Включить ADB по Wi-Fi (порт 5555)",
            descriptionEn = "Switches target adbd daemon to listen on TCP port 5555 (use from USB OTG).",
            descriptionRu = "Переключает демон adbd на прослушивание TCP-порта 5555 (удобно через USB OTG).",
            fullAdbSyntax = "adb tcpip 5555",
            shellCommand = null,
            specialAction = "tcpip"
        ),
        AdbCommandSpec(
            id = "conn_connect",
            category = AdbCommandCategory.CONNECTION,
            titleEn = "Connect via TCP/IP",
            titleRu = "Подключиться по IP",
            descriptionEn = "Connects ADB client to target device IP address and port.",
            descriptionRu = "Подключает ADB-клиент к устройству по IP-адресу и порту.",
            fullAdbSyntax = "adb connect 192.168.1.100:5555",
            shellCommand = null
        ),
        AdbCommandSpec(
            id = "conn_pair",
            category = AdbCommandCategory.CONNECTION,
            titleEn = "Pair Wireless Debugging",
            titleRu = "Сопряжение беспроводной отладки",
            descriptionEn = "Pairs with Android 11+ Wireless Debugging using pairing port and 6-digit code.",
            descriptionRu = "Сопряжение с Android 11+ по порту сопряжения и 6-значному коду.",
            fullAdbSyntax = "adb pair <ip>:<pairing_port> <code_6_digits>",
            shellCommand = null
        ),
        AdbCommandSpec(
            id = "conn_disconnect",
            category = AdbCommandCategory.CONNECTION,
            titleEn = "Disconnect TCP/IP target",
            titleRu = "Отключить TCP/IP устройство",
            descriptionEn = "Closes an active network ADB connection.",
            descriptionRu = "Закрывает активное сетевое ADB-подключение.",
            fullAdbSyntax = "adb disconnect 192.168.1.100:5555",
            shellCommand = null
        ),
        AdbCommandSpec(
            id = "conn_usb",
            category = AdbCommandCategory.CONNECTION,
            titleEn = "Switch daemon back to USB",
            titleRu = "Вернуть демон в режим USB",
            descriptionEn = "Restarts adbd in USB-only mode on the target device.",
            descriptionRu = "Перезапускает adbd на целевом устройстве только в USB-режим.",
            fullAdbSyntax = "adb usb",
            shellCommand = null
        ),
        AdbCommandSpec(
            id = "conn_kill_server",
            category = AdbCommandCategory.CONNECTION,
            titleEn = "Kill / Start ADB server",
            titleRu = "Перезапустить ADB-сервер",
            descriptionEn = "Stops and restarts the local ADB server daemon on a host PC.",
            descriptionRu = "Останавливает и запускает локальный демон ADB-сервера на ПК.",
            fullAdbSyntax = "adb kill-server && adb start-server",
            shellCommand = null
        ),
        AdbCommandSpec(
            id = "conn_forward",
            category = AdbCommandCategory.CONNECTION,
            titleEn = "Port forwarding (forward / reverse)",
            titleRu = "Проброс портов (forward / reverse)",
            descriptionEn = "Forwards local socket port to device port or reverse.",
            descriptionRu = "Пробрасывает локальный порт на порт устройства или обратно.",
            fullAdbSyntax = "adb forward tcp:8080 tcp:8080 | adb reverse tcp:8080 tcp:8080",
            shellCommand = null
        ),

        // PACKAGES (pm)
        AdbCommandSpec(
            id = "pm_list_all",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "List all packages with APK paths",
            titleRu = "Все пакеты приложений с путями APK",
            descriptionEn = "Displays every installed package and its APK path.",
            descriptionRu = "Выводит все установленные пакеты и пути к их APK-файлам.",
            fullAdbSyntax = "adb shell pm list packages -f",
            shellCommand = "pm list packages -f"
        ),
        AdbCommandSpec(
            id = "pm_list_3",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "List third-party user apps",
            titleRu = "Только пользовательские приложения",
            descriptionEn = "Shows only non-system apps installed by the user.",
            descriptionRu = "Показывает только сторонние приложения, установленные пользователем.",
            fullAdbSyntax = "adb shell pm list packages -3",
            shellCommand = "pm list packages -3"
        ),
        AdbCommandSpec(
            id = "pm_list_sys",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "List system packages",
            titleRu = "Список системных пакетов",
            descriptionEn = "Shows pre-installed system applications.",
            descriptionRu = "Выводит список предустановленных системных пакетов.",
            fullAdbSyntax = "adb shell pm list packages -s",
            shellCommand = "pm list packages -s"
        ),
        AdbCommandSpec(
            id = "pm_list_disabled",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "List disabled packages",
            titleRu = "Список отключённых пакетов",
            descriptionEn = "Lists packages that are currently disabled on the device.",
            descriptionRu = "Показывает приложения, которые отключены (заморожены).",
            fullAdbSyntax = "adb shell pm list packages -d",
            shellCommand = "pm list packages -d"
        ),
        AdbCommandSpec(
            id = "pm_install",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Install / update APK",
            titleRu = "Установить / обновить APK",
            descriptionEn = "Installs an APK (-r reinstall keeping data, -d allow downgrade, -g grant permissions).",
            descriptionRu = "Устанавливает APK (-r обновить с сохранением данных, -d откат версии, -g выдать права).",
            fullAdbSyntax = "adb install -r -g app.apk",
            shellCommand = "pm install -r /data/local/tmp/app.apk"
        ),
        AdbCommandSpec(
            id = "pm_uninstall",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Uninstall package",
            titleRu = "Удалить приложение",
            descriptionEn = "Removes a user application completely (or use --user 0 for system bloatware).",
            descriptionRu = "Полностью удаляет приложение (или --user 0 для системных приложений без root).",
            fullAdbSyntax = "adb uninstall com.example.app",
            shellCommand = "pm uninstall --user 0 com.example.app"
        ),
        AdbCommandSpec(
            id = "pm_clear",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Clear app data & cache",
            titleRu = "Очистить данные и кэш приложения",
            descriptionEn = "Deletes all user data and cache for a package.",
            descriptionRu = "Сбрасывает все данные и кэш указанного пакета.",
            fullAdbSyntax = "adb shell pm clear com.example.app",
            shellCommand = "pm clear com.example.app"
        ),
        AdbCommandSpec(
            id = "pm_disable",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Disable / Enable package",
            titleRu = "Отключить / Включить пакет",
            descriptionEn = "Disables or re-enables an app for user 0 without uninstalling.",
            descriptionRu = "Отключает или включает пакет для пользователя 0 без удаления.",
            fullAdbSyntax = "adb shell pm disable-user --user 0 <pkg>",
            shellCommand = "pm disable-user --user 0 com.example.app"
        ),
        AdbCommandSpec(
            id = "pm_grant",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Grant / Revoke runtime permission",
            titleRu = "Выдать / Отозвать разрешение",
            descriptionEn = "Grants or revokes a dangerous Android permission for an app.",
            descriptionRu = "Выдаёт или отзывает разрешение Android для указанного приложения.",
            fullAdbSyntax = "adb shell pm grant <pkg> android.permission.CAMERA",
            shellCommand = "pm grant com.example.app android.permission.CAMERA"
        ),

        // SHELL & SYSTEM INFO
        AdbCommandSpec(
            id = "sys_getprop",
            category = AdbCommandCategory.SHELL_SYSTEM,
            titleEn = "Read all system properties",
            titleRu = "Все системные свойства (getprop)",
            descriptionEn = "Prints Android build properties, model, SDK, ABI, and hardware flags.",
            descriptionRu = "Выводит свойства сборки Android, модель, SDK, архитектуру и параметры.",
            fullAdbSyntax = "adb shell getprop",
            shellCommand = "getprop"
        ),
        AdbCommandSpec(
            id = "sys_uname",
            category = AdbCommandCategory.SHELL_SYSTEM,
            titleEn = "Kernel version & architecture",
            titleRu = "Версия ядра Linux и архитектура",
            descriptionEn = "Shows kernel release, build date, and CPU architecture.",
            descriptionRu = "Показывает версию ядра и архитектуру процессора.",
            fullAdbSyntax = "adb shell uname -a",
            shellCommand = "uname -a"
        ),
        AdbCommandSpec(
            id = "sys_ps",
            category = AdbCommandCategory.SHELL_SYSTEM,
            titleEn = "List running processes",
            titleRu = "Список запущенных процессов",
            descriptionEn = "Lists active PIDs, UIDs, memory, and process names.",
            descriptionRu = "Показывает все активные процессы, PID и пользователей.",
            fullAdbSyntax = "adb shell ps -A",
            shellCommand = "ps -A"
        ),
        AdbCommandSpec(
            id = "sys_top",
            category = AdbCommandCategory.SHELL_SYSTEM,
            titleEn = "CPU & memory snapshot (top)",
            titleRu = "Нагрузка CPU и памяти (top)",
            descriptionEn = "Captures one iteration of top CPU-consuming processes.",
            descriptionRu = "Показывает снимок самых ресурсоёмких процессов по CPU и RAM.",
            fullAdbSyntax = "adb shell top -n 1 -m 15",
            shellCommand = "top -n 1 -m 15"
        ),
        AdbCommandSpec(
            id = "sys_df",
            category = AdbCommandCategory.SHELL_SYSTEM,
            titleEn = "Disk & partition usage",
            titleRu = "Свободное место и разделы (df)",
            descriptionEn = "Displays mounted filesystems and available storage space.",
            descriptionRu = "Отображает занятое и свободное место в разделах памяти.",
            fullAdbSyntax = "adb shell df -h",
            shellCommand = "df -h"
        ),
        AdbCommandSpec(
            id = "sys_ip",
            category = AdbCommandCategory.SHELL_SYSTEM,
            titleEn = "Network interfaces & IP addresses",
            titleRu = "Сетевые интерфейсы и IP-адреса",
            descriptionEn = "Shows wlan0/rmnet IP addresses and routing details.",
            descriptionRu = "Показывает IP-адреса Wi-Fi/мобильных интерфейсов.",
            fullAdbSyntax = "adb shell ip addr show",
            shellCommand = "ip addr show"
        ),
        AdbCommandSpec(
            id = "sys_free",
            category = AdbCommandCategory.SHELL_SYSTEM,
            titleEn = "RAM usage summary",
            titleRu = "Использование оперативной памяти",
            descriptionEn = "Prints total, used, free, and cached RAM.",
            descriptionRu = "Показывает общую, занятую и свободную оперативную память.",
            fullAdbSyntax = "adb shell free -h",
            shellCommand = "free -h"
        ),

        // ACTIVITY MANAGER (am)
        AdbCommandSpec(
            id = "am_start",
            category = AdbCommandCategory.ACTIVITY_MANAGER,
            titleEn = "Start activity or URL",
            titleRu = "Запустить Activity или ссылку",
            descriptionEn = "Launches a specific Activity or opens a URI intent.",
            descriptionRu = "Запускает конкретный экран приложения или открывает URL-намерение.",
            fullAdbSyntax = "adb shell am start -a android.intent.action.VIEW -d https://example.com",
            shellCommand = "am start -a android.settings.SETTINGS"
        ),
        AdbCommandSpec(
            id = "am_force_stop",
            category = AdbCommandCategory.ACTIVITY_MANAGER,
            titleEn = "Force stop application",
            titleRu = "Принудительно остановить приложение",
            descriptionEn = "Immediately terminates all processes of a package.",
            descriptionRu = "Мгновенно завершает все процессы указанного приложения.",
            fullAdbSyntax = "adb shell am force-stop com.example.app",
            shellCommand = "am force-stop com.example.app"
        ),
        AdbCommandSpec(
            id = "am_broadcast",
            category = AdbCommandCategory.ACTIVITY_MANAGER,
            titleEn = "Send system broadcast",
            titleRu = "Отправить Broadcast-событие",
            descriptionEn = "Dispatches an Intent broadcast to receivers.",
            descriptionRu = "Отправляет широковещательный Intent-сигнал.",
            fullAdbSyntax = "adb shell am broadcast -a com.example.ACTION",
            shellCommand = "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE"
        ),
        AdbCommandSpec(
            id = "am_top_activity",
            category = AdbCommandCategory.ACTIVITY_MANAGER,
            titleEn = "Show current foreground Activity",
            titleRu = "Текущая активная Activity на экране",
            descriptionEn = "Finds the currently resumed Activity package and class.",
            descriptionRu = "Определяет пакет и класс экрана, открытого сейчас на устройстве.",
            fullAdbSyntax = "adb shell dumpsys activity activities | grep mResumedActivity",
            shellCommand = "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'"
        ),

        // DISPLAY & SETTINGS
        AdbCommandSpec(
            id = "wm_size",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Get or change screen resolution",
            titleRu = "Разрешение экрана (wm size)",
            descriptionEn = "Inspects or overrides physical screen resolution (use 'wm size reset' to restore).",
            descriptionRu = "Показывает или меняет разрешение экрана (сброс: 'wm size reset').",
            fullAdbSyntax = "adb shell wm size [WxH | reset]",
            shellCommand = "wm size"
        ),
        AdbCommandSpec(
            id = "wm_density",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Get or change screen DPI",
            titleRu = "Плотность экрана DPI (wm density)",
            descriptionEn = "Inspects or sets custom DPI scaling (use 'wm density reset' to restore).",
            descriptionRu = "Показывает или меняет масштаб DPI (сброс: 'wm density reset').",
            fullAdbSyntax = "adb shell wm density [DPI | reset]",
            shellCommand = "wm density"
        ),
        AdbCommandSpec(
            id = "settings_anim",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Read animation scale settings",
            titleRu = "Скорость системных анимаций",
            descriptionEn = "Checks window, transition, and animator duration scales.",
            descriptionRu = "Проверяет масштаб скорости анимаций окон и переходов.",
            fullAdbSyntax = "adb shell settings get global animator_duration_scale",
            shellCommand = "settings get global window_animation_scale && settings get global transition_animation_scale && settings get global animator_duration_scale"
        ),
        AdbCommandSpec(
            id = "settings_list_system",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Dump global & system settings",
            titleRu = "Все системные настройки (settings list)",
            descriptionEn = "Lists Android global configuration keys and values.",
            descriptionRu = "Выводит список глобальных ключей настроек Android.",
            fullAdbSyntax = "adb shell settings list global",
            shellCommand = "settings list global"
        ),

        // FILES & SYNC
        AdbCommandSpec(
            id = "file_ls",
            category = AdbCommandCategory.FILES_SYNC,
            titleEn = "List /sdcard directory",
            titleRu = "Содержимое папки /sdcard",
            descriptionEn = "Lists files, permissions, and sizes in external storage.",
            descriptionRu = "Показывает файлы, права и размеры в /sdcard.",
            fullAdbSyntax = "adb shell ls -la /sdcard",
            shellCommand = "ls -la /sdcard"
        ),
        AdbCommandSpec(
            id = "file_push",
            category = AdbCommandCategory.FILES_SYNC,
            titleEn = "Push local file to device",
            titleRu = "Отправить файл на устройство (push)",
            descriptionEn = "Copies a file from host to the target device via ADB sync.",
            descriptionRu = "Копирует файл на устройство через протокол ADB sync (доступно во вкладке Инструменты).",
            fullAdbSyntax = "adb push local.txt /sdcard/Download/",
            shellCommand = null
        ),
        AdbCommandSpec(
            id = "file_pull",
            category = AdbCommandCategory.FILES_SYNC,
            titleEn = "Pull file from device",
            titleRu = "Скачать файл с устройства (pull)",
            descriptionEn = "Downloads a file from the target device via ADB sync.",
            descriptionRu = "Скачивает файл с устройства на этот телефон через ADB sync.",
            fullAdbSyntax = "adb pull /sdcard/Download/file.txt .",
            shellCommand = null
        ),

        // LOGS & DUMPSYS
        AdbCommandSpec(
            id = "log_recent",
            category = AdbCommandCategory.LOGS_DUMPSYS,
            titleEn = "Read recent Logcat (last 100 lines)",
            titleRu = "Последние 100 строк Logcat",
            descriptionEn = "Dumps the most recent system and app logs without blocking.",
            descriptionRu = "Выводит последние 100 строк системного журнала Logcat.",
            fullAdbSyntax = "adb logcat -d -t 100",
            shellCommand = "logcat -d -t 100"
        ),
        AdbCommandSpec(
            id = "log_clear",
            category = AdbCommandCategory.LOGS_DUMPSYS,
            titleEn = "Clear Logcat buffer",
            titleRu = "Очистить буфер Logcat",
            descriptionEn = "Flushes all stored Logcat ring buffers on the device.",
            descriptionRu = "Полностью очищает буферы журнала Logcat на устройстве.",
            fullAdbSyntax = "adb logcat -c",
            shellCommand = "logcat -c"
        ),
        AdbCommandSpec(
            id = "dump_battery",
            category = AdbCommandCategory.LOGS_DUMPSYS,
            titleEn = "Battery status & temperature",
            titleRu = "Статус батареи и температура",
            descriptionEn = "Shows charge level, voltage, temperature, health, and AC/USB state.",
            descriptionRu = "Показывает заряд, вольтаж, температуру, износ и источник питания.",
            fullAdbSyntax = "adb shell dumpsys battery",
            shellCommand = "dumpsys battery"
        ),
        AdbCommandSpec(
            id = "dump_meminfo",
            category = AdbCommandCategory.LOGS_DUMPSYS,
            titleEn = "System memory summary (meminfo)",
            titleRu = "Отчёт по памяти (dumpsys meminfo)",
            descriptionEn = "Shows RAM breakdown across processes and kernel.",
            descriptionRu = "Детальная сводка использования памяти по процессам.",
            fullAdbSyntax = "adb shell dumpsys meminfo",
            shellCommand = "dumpsys meminfo | head -n 45"
        ),
        AdbCommandSpec(
            id = "dump_services",
            category = AdbCommandCategory.LOGS_DUMPSYS,
            titleEn = "List all system services",
            titleRu = "Список системных служб (dumpsys -l)",
            descriptionEn = "Lists every service available to inspect with dumpsys.",
            descriptionRu = "Выводит все системные сервисы Android для диагностики.",
            fullAdbSyntax = "adb shell dumpsys -l",
            shellCommand = "dumpsys -l"
        ),

        // INPUT & SCREEN
        AdbCommandSpec(
            id = "input_screencap",
            category = AdbCommandCategory.INPUT_CONTROL,
            titleEn = "Capture device screenshot",
            titleRu = "Сделать скриншот экрана",
            descriptionEn = "Captures PNG framebuffer directly from the connected device.",
            descriptionRu = "Снимает PNG-скриншот экрана подключённого устройства.",
            fullAdbSyntax = "adb exec-out screencap -p > screen.png",
            shellCommand = null,
            specialAction = "screencap"
        ),
        AdbCommandSpec(
            id = "input_home",
            category = AdbCommandCategory.INPUT_CONTROL,
            titleEn = "Press HOME key",
            titleRu = "Нажать кнопку Домой (HOME)",
            descriptionEn = "Sends KEYCODE_HOME (3) to the device.",
            descriptionRu = "Отправляет нажатие кнопки Домой (KEYCODE_HOME).",
            fullAdbSyntax = "adb shell input keyevent 3",
            shellCommand = "input keyevent 3"
        ),
        AdbCommandSpec(
            id = "input_back",
            category = AdbCommandCategory.INPUT_CONTROL,
            titleEn = "Press BACK key",
            titleRu = "Нажать кнопку Назад (BACK)",
            descriptionEn = "Sends KEYCODE_BACK (4) to the device.",
            descriptionRu = "Отправляет нажатие кнопки Назад (KEYCODE_BACK).",
            fullAdbSyntax = "adb shell input keyevent 4",
            shellCommand = "input keyevent 4"
        ),
        AdbCommandSpec(
            id = "input_power",
            category = AdbCommandCategory.INPUT_CONTROL,
            titleEn = "Toggle screen / POWER key",
            titleRu = "Кнопка питания (POWER)",
            descriptionEn = "Wakes or locks the device screen (KEYCODE_POWER).",
            descriptionRu = "Блокирует или пробуждает экран (KEYCODE_POWER).",
            fullAdbSyntax = "adb shell input keyevent 26",
            shellCommand = "input keyevent 26"
        ),
        AdbCommandSpec(
            id = "input_text",
            category = AdbCommandCategory.INPUT_CONTROL,
            titleEn = "Type text remotely",
            titleRu = "Ввести текст удалённо",
            descriptionEn = "Injects text into the focused input field on the target device.",
            descriptionRu = "Вводит текст в активное поле ввода на подключённом устройстве.",
            fullAdbSyntax = "adb shell input text \"hello\"",
            shellCommand = "input text \"hello\""
        ),

        // POWER & BOOTLOADER
        AdbCommandSpec(
            id = "pwr_reboot",
            category = AdbCommandCategory.POWER_BOOTLOADER,
            titleEn = "Reboot device",
            titleRu = "Перезагрузить устройство",
            descriptionEn = "Reboots Android normally.",
            descriptionRu = "Обычная перезагрузка устройства.",
            fullAdbSyntax = "adb reboot",
            shellCommand = null,
            specialAction = "reboot"
        ),
        AdbCommandSpec(
            id = "pwr_bootloader",
            category = AdbCommandCategory.POWER_BOOTLOADER,
            titleEn = "Reboot to Bootloader / Fastboot",
            titleRu = "Перезагрузить в Bootloader / Fastboot",
            descriptionEn = "Restarts the device into bootloader mode.",
            descriptionRu = "Перезагружает устройство в режим загрузчика (Bootloader).",
            fullAdbSyntax = "adb reboot bootloader",
            shellCommand = null,
            specialAction = "reboot_bootloader"
        ),
        AdbCommandSpec(
            id = "pwr_recovery",
            category = AdbCommandCategory.POWER_BOOTLOADER,
            titleEn = "Reboot to Recovery",
            titleRu = "Перезагрузить в Recovery",
            descriptionEn = "Restarts the device into recovery mode.",
            descriptionRu = "Перезагружает устройство в режим восстановления (Recovery).",
            fullAdbSyntax = "adb reboot recovery",
            shellCommand = null,
            specialAction = "reboot_recovery"
        ),
        AdbCommandSpec(
            id = "pwr_sideload",
            category = AdbCommandCategory.POWER_BOOTLOADER,
            titleEn = "Reboot to Sideload (OTA)",
            titleRu = "Перезагрузить в Sideload (OTA)",
            descriptionEn = "Restarts into recovery sideload mode for flashing ZIP updates.",
            descriptionRu = "Перезагружает в режим ADB Sideload для прошивки OTA ZIP.",
            fullAdbSyntax = "adb reboot sideload",
            shellCommand = null,
            specialAction = "reboot_sideload"
        )
    )
}
