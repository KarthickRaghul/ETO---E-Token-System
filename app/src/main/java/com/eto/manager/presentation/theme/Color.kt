package com.eto.manager.presentation.theme

import androidx.compose.ui.graphics.Color

// --- Premium Design System Theme Colors ---

// Light Theme Palette (Primary reference style)
val LightBgStart = Color(0xFFEAF2FF)     // Pale lavender-blue
val LightBgEnd = Color(0xFFFFFFFF)       // White
val LightCardBg = Color(0xB3FFFFFF)      // White frosted glass (70% opacity)
val LightCardBorder = Color(0x66FFFFFF)  // Subtle white border (40% opacity)
val LightCardBorderAlt = Color(0x3364748B) // Subtle grey-slate border for extra definition
val LightTextPrimary = Color(0xFF0F172A)    // Slate 900
val LightTextSecondary = Color(0xFF475569)  // Slate 700
val LightTextTertiary = Color(0xFF64748B)   // Slate 500
val LightPrimaryBlue = Color(0xFF2563EB)    // SF Style Primary Blue
val LightSoftBlue = Color(0xFFEBF2FE)       // Selected background pill highlight

// Dark Theme Palette (Deep Navy glassmorphism style)
val DarkBgStart = Color(0xFF0A0F24)      // Deep Navy base
val DarkBgEnd = Color(0xFF131B3D)        // Midnight Blue base
val DarkCardBg = Color(0x801E293B)       // Translucent slate card (50% opacity)
val DarkCardBorder = Color(0x15FFFFFF)   // Very soft white border (8% opacity)
val DarkCardBorderAlt = Color(0x1A94A3B8) // Soft grey border for dark cards
val DarkTextPrimary = Color(0xFFF8FAFC)     // Slate 50
val DarkTextSecondary = Color(0xFFCBD5E1)   // Slate 300
val DarkTextTertiary = Color(0xFF94A3B8)    // Slate 400
val DarkPrimaryBlue = Color(0xFF3B82F6)     // High contrast active blue
val DarkSoftBlue = Color(0x263B82F6)        // Selected background pill highlight (15% opacity)

// Soft Status Colors (Light Theme)
val LightSuccessBg = Color(0xFFD1FAE5)
val LightSuccessText = Color(0xFF065F46)
val LightWarningBg = Color(0xFFFEF3C7)
val LightWarningText = Color(0xFF92400E)
val LightErrorBg = Color(0xFFFEE2E2)
val LightErrorText = Color(0xFF991B1B)

// Soft Status Colors (Dark Theme)
val DarkSuccessBg = Color(0xFF064E3B)
val DarkSuccessText = Color(0xFF34D399)
val DarkWarningBg = Color(0xFF78350F)
val DarkWarningText = Color(0xFFFBBF24)
val DarkErrorBg = Color(0xFF7F1D1D)
val DarkErrorText = Color(0xFFF87171)

// Shared/Legacy Color Definitions (to avoid compile issues with existing models)
val SuccessGreen = Color(0xFF10B981)
val WarningOrange = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)
val SteelBlueMedium = Color(0xFF266CA9)
val SkyBlueLight = Color(0xFFADE1FB)
val NavyBluePrimary = Color(0xFF0F2573)
val MidnightDarkBg = Color(0xFF01082D)
val SurfaceGray = Color(0xFF1E293B)
val TextWhite = Color(0xFFF8FAFC)
val TextGray = Color(0xFF94A3B8)
