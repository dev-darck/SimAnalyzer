package com.project.analyzer.ac.telemetry.impl.fallback.logfile.model

internal object RegexConst {

    val RE_TIMESTAMP = Regex(
        pattern = """^\s*\[(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+)]"""
    )
    val RE_PHYSICS_TRACK = Regex(
        pattern = """Creating physics track:\s*(.+)$""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_TRACK_SLUG = Regex(
        pattern = """\bTRACK NAME\b\s+(.+)$""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_CONTAINER = Regex(
        pattern = """content[\\/]+tracks[\\/]+([^\\/]+)[\\/]+containers[\\/]+layout_([^\\/.]+)\.scene""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_CONNECTING_GAMECAR = Regex(
        pattern = """connecting\s+gamecar\s+([0-9a-fA-F-]+)\s*\(([^|]+)\|\s*(\d*)\)""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_CONNECTED_ON_CAR_WITH_UUID = Regex(
        pattern = """\bconnected\s+on\s+car\s+(\S+),\s*with\s+new\s+carId\s+([0-9a-fA-F-]+)""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_AI_DRIVER_EVO = Regex(
        pattern = """Creating\s+AiDriverEvo\s+for\s+car\s+(\S+)\s*\(([0-9a-fA-F-]+)\)""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_SESSION_TYPE = Regex(
        pattern = """\b([A-Za-z0-9_]+Remote)\s+([A-Za-z0-9_]+)\s+created\b""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_PLAYER_COMMAND = Regex(
        pattern = """onSetPlayerCurrentCarCommand:.*content[\\/]+cars[\\/]+([^\\/]+)[\\/]""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_DRIVER_ON_CAR = Regex(
        pattern = """\bDriver\s+\S+\s+on\s+car\s+(\S+)""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_MY_CAR = Regex(
        pattern = """Ai\s+car\s+choice:\s*my\s+car:\s*(\S+)""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_CAR_DISPLAY = Regex(
        pattern = """CarDisplay\.init:(\S+)""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_DRIVER = Regex(
        pattern = """connecting gamecar.*\((.+?)\s*\|\s*(\d*)\)""",
        option = RegexOption.IGNORE_CASE
    )
    val RE_PENALTY_KEY = Regex(
        pattern = """\{PENALTY_ADDED_KEY}\s*#(\d+)""",
        option = RegexOption.IGNORE_CASE
    )
}
