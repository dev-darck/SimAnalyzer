package com.project.analyzer.theme.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private fun sansStyle(
    fonts: SimAnalyzerFontFamilies,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp,
): TextStyle = TextStyle(
    fontFamily = fonts.sans,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

public fun simAnalyzerTypography(
    fonts: SimAnalyzerFontFamilies = DefaultSimAnalyzerFontFamilies,
): Typography = Typography(
    displayLarge = sansStyle(fonts, FontWeight.Bold, 64.sp, 68.sp),
    displayMedium = sansStyle(fonts, FontWeight.Bold, 58.sp, 62.sp),
    displaySmall = sansStyle(fonts, FontWeight.SemiBold, 22.sp, 28.sp),
    headlineLarge = sansStyle(fonts, FontWeight.SemiBold, 32.sp, 38.sp),
    headlineMedium = sansStyle(fonts, FontWeight.SemiBold, 24.sp, 30.sp),
    headlineSmall = sansStyle(fonts, FontWeight.SemiBold, 20.sp, 26.sp),
    titleLarge = sansStyle(fonts, FontWeight.SemiBold, 22.sp, 28.sp),
    titleMedium = sansStyle(fonts, FontWeight.SemiBold, 20.sp, 26.sp),
    titleSmall = sansStyle(fonts, FontWeight.SemiBold, 16.sp, 22.sp),
    bodyLarge = sansStyle(fonts, FontWeight.Medium, 14.sp, 20.sp),
    bodyMedium = sansStyle(fonts, FontWeight.Normal, 14.sp, 20.sp),
    bodySmall = sansStyle(fonts, FontWeight.Normal, 12.sp, 18.sp),
    labelLarge = sansStyle(fonts, FontWeight.Medium, 14.sp, 18.sp),
    labelMedium = sansStyle(fonts, FontWeight.Medium, 12.sp, 16.sp),
    labelSmall = sansStyle(fonts, FontWeight.Medium, 11.sp, 14.sp),
)
