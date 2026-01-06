package com.project.analyzer.api.di

public interface AppEnvironment {
    public val isDev: Boolean
    public val osName: String
    public val isWindows: Boolean
    public val userHome: String
}
