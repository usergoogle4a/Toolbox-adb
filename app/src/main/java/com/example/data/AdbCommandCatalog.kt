package com.example.data

enum class AdbCommandCategory {
    CONNECTION,
    MONKEY_STRESS,
    PACKAGES,
    SHELL_SYSTEM,
    ACTIVITY_MANAGER,
    DISPLAY_SETTINGS,
    NETWORK_RADIO,
    FILES_SYNC,
    LOGS_DUMPSYS,
    INPUT_CONTROL,
    POWER_BOOTLOADER
}

data class CommandParam(
    val key: String,
    val labelEn: String,
    val labelRu: String,
    val exampleHint: String,
    val isNumeric: Boolean = false,
    val isPackagePicker: Boolean = false
)

data class AdbCommandSpec(
    val id: String,
    val category: AdbCommandCategory,
    val titleEn: String,
    val titleRu: String,
    val descriptionEn: String,
    val descriptionRu: String,
    val fullAdbSyntax: String,
    val shellCommand: String?, // may contain {param} placeholders
    val parameters: List<CommandParam> = emptyList(),
    val specialAction: String? = null
) {
    fun renderShellCommand(values: Map<String, String>): String? {
        val tpl = shellCommand ?: return null
        var rendered = tpl
        for ((k, v) in values) {
            rendered = rendered.replace("{$k}", v.trim())
        }
        return rendered
    }
}

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
            fullAdbSyntax = "adb connect <ip>:<port>",
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
            fullAdbSyntax = "adb disconnect <ip>:<port>",
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
            fullAdbSyntax = "adb forward tcp:<local> tcp:<remote>",
            shellCommand = null
        ),

        // MONKEY & STRESS TESTING
        AdbCommandSpec(
            id = "monkey_app",
            category = AdbCommandCategory.MONKEY_STRESS,
            titleEn = "Monkey stress test for app",
            titleRu = "Monkey-тест приложения (случайные нажатия)",
            descriptionEn = "Sends pseudo-random UI taps, swipes, and key events to a package.",
            descriptionRu = "Отправляет поток случайных нажатий, свайпов и событий в выбранное приложение.",
            fullAdbSyntax = "adb shell monkey -p <package> -v <events>",
            shellCommand = "monkey -p {package} -v {events}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.android.settings", isNumeric = false, isPackagePicker = true),
                CommandParam("events", "Number of events", "Количество событий", "500", isNumeric = true)
            )
        ),
        AdbCommandSpec(
            id = "monkey_throttle",
            category = AdbCommandCategory.MONKEY_STRESS,
            titleEn = "Monkey test with delay (throttle)",
            titleRu = "Monkey-тест с задержкой между действиями",
            descriptionEn = "Runs Monkey UI fuzzer with a fixed millisecond delay between events.",
            descriptionRu = "Запускает Monkey с заданной паузой (мс) между случайными действиями.",
            fullAdbSyntax = "adb shell monkey -p <package> --throttle <ms> -v <events>",
            shellCommand = "monkey -p {package} --throttle {ms} -v {events}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.android.settings", isNumeric = false, isPackagePicker = true),
                CommandParam("ms", "Delay (ms)", "Задержка (мс)", "150", isNumeric = true),
                CommandParam("events", "Number of events", "Количество событий", "200", isNumeric = true)
            )
        ),
        AdbCommandSpec(
            id = "monkey_system",
            category = AdbCommandCategory.MONKEY_STRESS,
            titleEn = "System-wide Monkey test",
            titleRu = "Глобальный Monkey-тест всей системы",
            descriptionEn = "Generates random UI events across the entire system ignoring crashes.",
            descriptionRu = "Генерирует случайные события по всему интерфейсу системы.",
            fullAdbSyntax = "adb shell monkey --ignore-crashes --ignore-timeouts -v <events>",
            shellCommand = "monkey --ignore-crashes --ignore-timeouts -v {events}",
            parameters = listOf(
                CommandParam("events", "Number of events", "Количество событий", "300", isNumeric = true)
            )
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
            id = "pm_path",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Get APK path of package",
            titleRu = "Путь к APK конкретного пакета",
            descriptionEn = "Prints the filesystem path of a package's base and split APKs.",
            descriptionRu = "Показывает точный путь к APK-файлу выбранного пакета.",
            fullAdbSyntax = "adb shell pm path <package>",
            shellCommand = "pm path {package}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.android.chrome", isPackagePicker = true)
            )
        ),
        AdbCommandSpec(
            id = "pm_clear",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Clear app data & cache",
            titleRu = "Очистить данные и кэш приложения",
            descriptionEn = "Deletes all user data and cache for a package.",
            descriptionRu = "Сбрасывает все данные и кэш указанного пакета.",
            fullAdbSyntax = "adb shell pm clear <package>",
            shellCommand = "pm clear {package}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.example.app", isPackagePicker = true)
            )
        ),
        AdbCommandSpec(
            id = "pm_uninstall",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Uninstall package (User 0)",
            titleRu = "Удалить приложение (User 0)",
            descriptionEn = "Removes a package for user 0 (works for system bloatware without root).",
            descriptionRu = "Удаляет пакет для пользователя 0 (работает и для системных приложений без root).",
            fullAdbSyntax = "adb shell pm uninstall --user 0 <package>",
            shellCommand = "pm uninstall --user 0 {package}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.example.app", isPackagePicker = true)
            )
        ),
        AdbCommandSpec(
            id = "pm_disable",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Disable package (freeze)",
            titleRu = "Отключить / заморозить пакет",
            descriptionEn = "Disables an app for user 0 without uninstalling.",
            descriptionRu = "Отключает пакет для пользователя 0 без удаления.",
            fullAdbSyntax = "adb shell pm disable-user --user 0 <package>",
            shellCommand = "pm disable-user --user 0 {package}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.example.app", isPackagePicker = true)
            )
        ),
        AdbCommandSpec(
            id = "pm_enable",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Enable disabled package",
            titleRu = "Включить отключённый пакет",
            descriptionEn = "Re-enables a previously disabled package.",
            descriptionRu = "Снова включает ранее отключённое приложение.",
            fullAdbSyntax = "adb shell pm enable <package>",
            shellCommand = "pm enable {package}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.example.app", isPackagePicker = true)
            )
        ),
        AdbCommandSpec(
            id = "pm_grant",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Grant runtime permission",
            titleRu = "Выдать разрешение приложению",
            descriptionEn = "Grants a dangerous Android permission for an app.",
            descriptionRu = "Выдаёт указанное Android-разрешение приложению.",
            fullAdbSyntax = "adb shell pm grant <package> <permission>",
            shellCommand = "pm grant {package} {permission}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.example.app", isPackagePicker = true),
                CommandParam("permission", "Permission", "Разрешение", "android.permission.CAMERA")
            )
        ),
        AdbCommandSpec(
            id = "pm_revoke",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Revoke runtime permission",
            titleRu = "Отозвать разрешение у приложения",
            descriptionEn = "Revokes a previously granted runtime permission.",
            descriptionRu = "Отзывает ранее выданное разрешение у пакета.",
            fullAdbSyntax = "adb shell pm revoke <package> <permission>",
            shellCommand = "pm revoke {package} {permission}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.example.app", isPackagePicker = true),
                CommandParam("permission", "Permission", "Разрешение", "android.permission.CAMERA")
            )
        ),
        AdbCommandSpec(
            id = "pm_compile_speed",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "AOT compile app (speed)",
            titleRu = "Скомпилировать приложение (ART speed)",
            descriptionEn = "Forces ART ahead-of-time compilation in speed mode for smoother performance.",
            descriptionRu = "Принудительно компилирует код пакета в профиль speed для ускорения запуска.",
            fullAdbSyntax = "adb shell cmd package compile -m speed -f <package>",
            shellCommand = "cmd package compile -m speed -f {package}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.example.app", isPackagePicker = true)
            )
        ),
        AdbCommandSpec(
            id = "pm_trim_caches",
            category = AdbCommandCategory.PACKAGES,
            titleEn = "Trim system app caches",
            titleRu = "Очистить кэши до заданного объёма",
            descriptionEn = "Requests Android to free cache files up to target size in MB.",
            descriptionRu = "Освобождает кэш приложений до указанного объёма в мегабайтах.",
            fullAdbSyntax = "adb shell pm trim-caches <mb>M",
            shellCommand = "pm trim-caches {mb}M",
            parameters = listOf(
                CommandParam("mb", "Size (MB)", "Объём (МБ)", "512", isNumeric = true)
            )
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
            descriptionEn = "Captures one snapshot of top N CPU-consuming processes.",
            descriptionRu = "Показывает снимок самых ресурсоёмких процессов по CPU и RAM.",
            fullAdbSyntax = "adb shell top -n 1 -m <count>",
            shellCommand = "top -n 1 -m {count}",
            parameters = listOf(
                CommandParam("count", "Process count", "Количество процессов", "15", isNumeric = true)
            )
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
            id = "sys_free",
            category = AdbCommandCategory.SHELL_SYSTEM,
            titleEn = "RAM usage summary",
            titleRu = "Использование оперативной памяти",
            descriptionEn = "Prints total, used, free, and cached RAM.",
            descriptionRu = "Показывает общую, занятую и свободную оперативную память.",
            fullAdbSyntax = "adb shell free -h",
            shellCommand = "free -h"
        ),
        AdbCommandSpec(
            id = "sys_cpuinfo",
            category = AdbCommandCategory.SHELL_SYSTEM,
            titleEn = "Read CPU hardware info",
            titleRu = "Информация о процессоре (/proc/cpuinfo)",
            descriptionEn = "Displays processor cores, features, and hardware platform.",
            descriptionRu = "Выводит данные о ядрах процессора и аппаратной платформе.",
            fullAdbSyntax = "adb shell cat /proc/cpuinfo",
            shellCommand = "cat /proc/cpuinfo"
        ),
        AdbCommandSpec(
            id = "sys_uptime",
            category = AdbCommandCategory.SHELL_SYSTEM,
            titleEn = "Device uptime & load average",
            titleRu = "Время работы и средняя нагрузка (uptime)",
            descriptionEn = "Prints how long the device has been running since boot.",
            descriptionRu = "Показывает время работы устройства без перезагрузки и load average.",
            fullAdbSyntax = "adb shell uptime",
            shellCommand = "uptime"
        ),

        // ACTIVITY MANAGER (am)
        AdbCommandSpec(
            id = "am_start_url",
            category = AdbCommandCategory.ACTIVITY_MANAGER,
            titleEn = "Open URL on device",
            titleRu = "Открыть ссылку на устройстве",
            descriptionEn = "Launches browser or app handler for an HTTP/HTTPS URL.",
            descriptionRu = "Открывает указанный URL в браузере или обработчике на устройстве.",
            fullAdbSyntax = "adb shell am start -a android.intent.action.VIEW -d <url>",
            shellCommand = "am start -a android.intent.action.VIEW -d {url}",
            parameters = listOf(
                CommandParam("url", "URL", "Ссылка", "https://example.com")
            )
        ),
        AdbCommandSpec(
            id = "am_start_pkg",
            category = AdbCommandCategory.ACTIVITY_MANAGER,
            titleEn = "Launch app by package",
            titleRu = "Запустить приложение по имени пакета",
            descriptionEn = "Starts the launcher activity of the specified package.",
            descriptionRu = "Запускает главный экран указанного пакета.",
            fullAdbSyntax = "adb shell monkey -p <package> -c android.intent.category.LAUNCHER 1",
            shellCommand = "monkey -p {package} -c android.intent.category.LAUNCHER 1",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.android.settings", isPackagePicker = true)
            )
        ),
        AdbCommandSpec(
            id = "am_force_stop",
            category = AdbCommandCategory.ACTIVITY_MANAGER,
            titleEn = "Force stop application",
            titleRu = "Принудительно остановить приложение",
            descriptionEn = "Immediately terminates all processes of a package.",
            descriptionRu = "Мгновенно завершает все процессы указанного приложения.",
            fullAdbSyntax = "adb shell am force-stop <package>",
            shellCommand = "am force-stop {package}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.example.app", isPackagePicker = true)
            )
        ),
        AdbCommandSpec(
            id = "am_kill_all",
            category = AdbCommandCategory.ACTIVITY_MANAGER,
            titleEn = "Kill all background processes",
            titleRu = "Завершить все фоновые процессы (kill-all)",
            descriptionEn = "Frees RAM by killing safe-to-stop background processes.",
            descriptionRu = "Освобождает ОЗУ, завершая фоновые процессы.",
            fullAdbSyntax = "adb shell am kill-all",
            shellCommand = "am kill-all"
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
            id = "wm_size_info",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Check screen resolution",
            titleRu = "Текущее разрешение экрана (wm size)",
            descriptionEn = "Reads physical and override screen resolution.",
            descriptionRu = "Показывает физическое и переопределённое разрешение экрана.",
            fullAdbSyntax = "adb shell wm size",
            shellCommand = "wm size"
        ),
        AdbCommandSpec(
            id = "wm_size_set",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Set custom screen resolution",
            titleRu = "Изменить разрешение экрана (ШxВ)",
            descriptionEn = "Overrides display resolution to width x height pixels.",
            descriptionRu = "Задаёт новое разрешение экрана (ширина и высота в пикселях).",
            fullAdbSyntax = "adb shell wm size <width>x<height>",
            shellCommand = "wm size {width}x{height}",
            parameters = listOf(
                CommandParam("width", "Width (px)", "Ширина (px)", "1080", isNumeric = true),
                CommandParam("height", "Height (px)", "Высота (px)", "2400", isNumeric = true)
            )
        ),
        AdbCommandSpec(
            id = "wm_size_reset",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Reset screen resolution",
            titleRu = "Сбросить разрешение экрана",
            descriptionEn = "Restores default physical display resolution.",
            descriptionRu = "Возвращает стандартное разрешение экрана.",
            fullAdbSyntax = "adb shell wm size reset",
            shellCommand = "wm size reset"
        ),
        AdbCommandSpec(
            id = "wm_density_set",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Set custom screen DPI",
            titleRu = "Изменить плотность экрана (DPI)",
            descriptionEn = "Overrides UI scaling DPI (use 'wm density reset' to restore).",
            descriptionRu = "Меняет масштаб интерфейса через значение DPI.",
            fullAdbSyntax = "adb shell wm density <dpi>",
            shellCommand = "wm density {dpi}",
            parameters = listOf(
                CommandParam("dpi", "DPI value", "Значение DPI", "420", isNumeric = true)
            )
        ),
        AdbCommandSpec(
            id = "wm_density_reset",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Reset screen DPI",
            titleRu = "Сбросить DPI экрана",
            descriptionEn = "Restores factory display density.",
            descriptionRu = "Возвращает заводское значение DPI.",
            fullAdbSyntax = "adb shell wm density reset",
            shellCommand = "wm density reset"
        ),
        AdbCommandSpec(
            id = "settings_screen_timeout",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Set screen-off timeout (ms)",
            titleRu = "Таймаут выключения экрана (мс)",
            descriptionEn = "Sets inactivity timeout before screen turns off (e.g. 60000 for 1 min).",
            descriptionRu = "Задаёт время до выключения экрана в миллисекундах (60000 = 1 мин).",
            fullAdbSyntax = "adb shell settings put system screen_off_timeout <ms>",
            shellCommand = "settings put system screen_off_timeout {ms}",
            parameters = listOf(
                CommandParam("ms", "Timeout (ms)", "Таймаут (мс)", "60000", isNumeric = true)
            )
        ),
        AdbCommandSpec(
            id = "settings_list_global",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "List global settings",
            titleRu = "Все глобальные настройки (settings list global)",
            descriptionEn = "Lists Android global configuration keys and values.",
            descriptionRu = "Выводит список глобальных ключей настроек Android.",
            fullAdbSyntax = "adb shell settings list global",
            shellCommand = "settings list global"
        ),
        AdbCommandSpec(
            id = "statusbar_expand",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Expand notifications shade",
            titleRu = "Открыть шторку уведомлений",
            descriptionEn = "Pulls down the system notification panel.",
            descriptionRu = "Программно раскрывает шторку уведомлений.",
            fullAdbSyntax = "adb shell cmd statusbar expand-notifications",
            shellCommand = "cmd statusbar expand-notifications"
        ),
        AdbCommandSpec(
            id = "statusbar_collapse",
            category = AdbCommandCategory.DISPLAY_SETTINGS,
            titleEn = "Collapse notifications shade",
            titleRu = "Закрыть шторку уведомлений",
            descriptionEn = "Closes the notification shade.",
            descriptionRu = "Сворачивает шторку уведомлений.",
            fullAdbSyntax = "adb shell cmd statusbar collapse",
            shellCommand = "cmd statusbar collapse"
        ),

        // NETWORK & RADIO
        AdbCommandSpec(
            id = "net_ip",
            category = AdbCommandCategory.NETWORK_RADIO,
            titleEn = "Network interfaces & IP addresses",
            titleRu = "Сетевые интерфейсы и IP-адреса",
            descriptionEn = "Shows wlan0/rmnet IP addresses and routing details.",
            descriptionRu = "Показывает IP-адреса Wi-Fi и мобильных интерфейсов.",
            fullAdbSyntax = "adb shell ip addr show",
            shellCommand = "ip addr show"
        ),
        AdbCommandSpec(
            id = "net_wifi_on",
            category = AdbCommandCategory.NETWORK_RADIO,
            titleEn = "Enable Wi-Fi",
            titleRu = "Включить Wi-Fi",
            descriptionEn = "Turns on Wi-Fi radio on the connected device.",
            descriptionRu = "Включает модуль Wi-Fi на подключённом устройстве.",
            fullAdbSyntax = "adb shell svc wifi enable",
            shellCommand = "svc wifi enable"
        ),
        AdbCommandSpec(
            id = "net_wifi_off",
            category = AdbCommandCategory.NETWORK_RADIO,
            titleEn = "Disable Wi-Fi",
            titleRu = "Выключить Wi-Fi",
            descriptionEn = "Turns off Wi-Fi radio on the connected device.",
            descriptionRu = "Отключает модуль Wi-Fi на устройстве.",
            fullAdbSyntax = "adb shell svc wifi disable",
            shellCommand = "svc wifi disable"
        ),
        AdbCommandSpec(
            id = "net_data_on",
            category = AdbCommandCategory.NETWORK_RADIO,
            titleEn = "Enable Mobile Data",
            titleRu = "Включить мобильный интернет",
            descriptionEn = "Turns cellular mobile data on.",
            descriptionRu = "Включает передачу мобильных данных.",
            fullAdbSyntax = "adb shell svc data enable",
            shellCommand = "svc data enable"
        ),
        AdbCommandSpec(
            id = "net_data_off",
            category = AdbCommandCategory.NETWORK_RADIO,
            titleEn = "Disable Mobile Data",
            titleRu = "Выключить мобильный интернет",
            descriptionEn = "Turns cellular mobile data off.",
            descriptionRu = "Отключает передачу мобильных данных.",
            fullAdbSyntax = "adb shell svc data disable",
            shellCommand = "svc data disable"
        ),
        AdbCommandSpec(
            id = "net_ping",
            category = AdbCommandCategory.NETWORK_RADIO,
            titleEn = "Ping host from device",
            titleRu = "Ping узла с устройства",
            descriptionEn = "Sends N ICMP ping packets from target device to a host.",
            descriptionRu = "Отправляет N ICMP-пакетов с целевого устройства к указанному хосту.",
            fullAdbSyntax = "adb shell ping -c <count> <host>",
            shellCommand = "ping -c {count} {host}",
            parameters = listOf(
                CommandParam("count", "Packet count", "Число пакетов", "4", isNumeric = true),
                CommandParam("host", "Host / IP", "Адрес / домен", "8.8.8.8")
            )
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
            descriptionEn = "Copies a file from host to the target device via ADB sync (available in Device & Files).",
            descriptionRu = "Копирует файл на устройство через ADB sync (во вкладке «Устройство и Файлы»).",
            fullAdbSyntax = "adb push <local_file> /sdcard/Download/",
            shellCommand = null
        ),
        AdbCommandSpec(
            id = "file_pull",
            category = AdbCommandCategory.FILES_SYNC,
            titleEn = "Pull file from device",
            titleRu = "Скачать файл с устройства (pull)",
            descriptionEn = "Downloads a file from the target device via ADB sync.",
            descriptionRu = "Скачивает файл с устройства через ADB sync.",
            fullAdbSyntax = "adb pull /sdcard/Download/<file> .",
            shellCommand = null
        ),

        // LOGS & DUMPSYS
        AdbCommandSpec(
            id = "log_recent",
            category = AdbCommandCategory.LOGS_DUMPSYS,
            titleEn = "Read recent Logcat lines",
            titleRu = "Последние N строк Logcat",
            descriptionEn = "Dumps the most recent system and app log lines.",
            descriptionRu = "Выводит заданное количество последних строк журнала Logcat.",
            fullAdbSyntax = "adb logcat -d -t <lines>",
            shellCommand = "logcat -d -t {lines}",
            parameters = listOf(
                CommandParam("lines", "Line count", "Количество строк", "100", isNumeric = true)
            )
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
            id = "dump_battery_set",
            category = AdbCommandCategory.LOGS_DUMPSYS,
            titleEn = "Simulate battery percentage",
            titleRu = "Имитация процента заряда батареи",
            descriptionEn = "Temporarily overrides reported battery level (0-100) for testing.",
            descriptionRu = "Временно задаёт фиктивный процент заряда (0–100) для тестов.",
            fullAdbSyntax = "adb shell dumpsys battery set level <percent>",
            shellCommand = "dumpsys battery set level {percent}",
            parameters = listOf(
                CommandParam("percent", "Battery % (0-100)", "Заряд % (0-100)", "15", isNumeric = true)
            )
        ),
        AdbCommandSpec(
            id = "dump_battery_reset",
            category = AdbCommandCategory.LOGS_DUMPSYS,
            titleEn = "Reset battery simulation",
            titleRu = "Сбросить имитацию батареи",
            descriptionEn = "Restores real hardware battery reporting.",
            descriptionRu = "Возвращает отображение реального заряда батареи.",
            fullAdbSyntax = "adb shell dumpsys battery reset",
            shellCommand = "dumpsys battery reset"
        ),
        AdbCommandSpec(
            id = "dump_cpuinfo",
            category = AdbCommandCategory.LOGS_DUMPSYS,
            titleEn = "CPU usage by process",
            titleRu = "Нагрузка CPU по процессам (cpuinfo)",
            descriptionEn = "Shows recent CPU percentage breakdown per process.",
            descriptionRu = "Показывает процент загрузки CPU каждым процессом.",
            fullAdbSyntax = "adb shell dumpsys cpuinfo",
            shellCommand = "dumpsys cpuinfo"
        ),
        AdbCommandSpec(
            id = "dump_gfxinfo",
            category = AdbCommandCategory.LOGS_DUMPSYS,
            titleEn = "UI frame stats & jank (gfxinfo)",
            titleRu = "Кадры и лаги рендеринга (gfxinfo)",
            descriptionEn = "Displays frame rendering timings and jank percentage for an app.",
            descriptionRu = "Показывает статистику FPS, время кадров и лаги (jank) приложения.",
            fullAdbSyntax = "adb shell dumpsys gfxinfo <package>",
            shellCommand = "dumpsys gfxinfo {package}",
            parameters = listOf(
                CommandParam("package", "Package name", "Имя пакета", "com.example.app", isPackagePicker = true)
            )
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
            id = "input_tap",
            category = AdbCommandCategory.INPUT_CONTROL,
            titleEn = "Tap screen coordinates (X, Y)",
            titleRu = "Нажатие по координатам (X, Y)",
            descriptionEn = "Simulates a single touch tap at (x, y) pixels.",
            descriptionRu = "Имитирует нажатие на экран в точке (X, Y).",
            fullAdbSyntax = "adb shell input tap <x> <y>",
            shellCommand = "input tap {x} {y}",
            parameters = listOf(
                CommandParam("x", "X coordinate", "Координата X", "540", isNumeric = true),
                CommandParam("y", "Y coordinate", "Координата Y", "1200", isNumeric = true)
            )
        ),
        AdbCommandSpec(
            id = "input_swipe",
            category = AdbCommandCategory.INPUT_CONTROL,
            titleEn = "Swipe gesture (X1,Y1 -> X2,Y2)",
            titleRu = "Свайп по координатам",
            descriptionEn = "Performs a touch swipe from start to end coordinates over duration ms.",
            descriptionRu = "Выполняет свайп из начальной точки в конечную за заданное время (мс).",
            fullAdbSyntax = "adb shell input swipe <x1> <y1> <x2> <y2> <ms>",
            shellCommand = "input swipe {x1} {y1} {x2} {y2} {ms}",
            parameters = listOf(
                CommandParam("x1", "Start X", "Начало X", "540", isNumeric = true),
                CommandParam("y1", "Start Y", "Начало Y", "1600", isNumeric = true),
                CommandParam("x2", "End X", "Конец X", "540", isNumeric = true),
                CommandParam("y2", "End Y", "Конец Y", "600", isNumeric = true),
                CommandParam("ms", "Duration (ms)", "Длительность (мс)", "300", isNumeric = true)
            )
        ),
        AdbCommandSpec(
            id = "input_keyevent",
            category = AdbCommandCategory.INPUT_CONTROL,
            titleEn = "Send custom KeyEvent code",
            titleRu = "Отправить код клавиши (keyevent)",
            descriptionEn = "Sends any Android KeyEvent numeric code (3=Home, 4=Back, 26=Power, 82=Menu).",
            descriptionRu = "Отправляет любой числовой код клавиши (3=Home, 4=Back, 26=Power, 66=Enter).",
            fullAdbSyntax = "adb shell input keyevent <keycode>",
            shellCommand = "input keyevent {keycode}",
            parameters = listOf(
                CommandParam("keycode", "KeyEvent code", "Код клавиши", "3", isNumeric = true)
            )
        ),
        AdbCommandSpec(
            id = "input_text",
            category = AdbCommandCategory.INPUT_CONTROL,
            titleEn = "Type text remotely",
            titleRu = "Ввести текст удалённо",
            descriptionEn = "Injects text into the focused input field on the target device.",
            descriptionRu = "Вводит текст в активное поле ввода на подключённом устройстве.",
            fullAdbSyntax = "adb shell input text \"<text>\"",
            shellCommand = "input text \"{text}\"",
            parameters = listOf(
                CommandParam("text", "Text to type", "Текст для ввода", "hello")
            )
        ),
        AdbCommandSpec(
            id = "input_screenrecord",
            category = AdbCommandCategory.INPUT_CONTROL,
            titleEn = "Record screen video to /sdcard",
            titleRu = "Запись видео с экрана (/sdcard/record.mp4)",
            descriptionEn = "Records screen video for N seconds to /sdcard/record.mp4.",
            descriptionRu = "Записывает видео с экрана длительностью N секунд в /sdcard/record.mp4.",
            fullAdbSyntax = "adb shell screenrecord --time-limit <seconds> /sdcard/record.mp4",
            shellCommand = "screenrecord --time-limit {seconds} /sdcard/record.mp4",
            parameters = listOf(
                CommandParam("seconds", "Duration (sec)", "Длительность (сек)", "10", isNumeric = true)
            )
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
