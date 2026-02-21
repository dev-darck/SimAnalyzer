package com.project.analyzer.leak.api

public object LeakCanaryRuntime {

    @Volatile
    private var controller: LeakCanaryController? = null

    public fun install(controller: LeakCanaryController) {
        this.controller = controller
    }

    public fun uninstall(controller: LeakCanaryController? = null) {
        val current = this.controller
        if (controller == null || controller === current) {
            this.controller = null
        }
    }

    public fun watch(watchedObject: Any, description: String = "") {
        controller?.watch(watchedObject, description)
    }

    public fun dumpNow(reason: String? = null) {
        controller?.dumpNow(reason)
    }

    public val isInstalled: Boolean
        get() = controller != null
}
