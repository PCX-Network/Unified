/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Unified internationalization (i18n) and localization framework.
 *
 * <p>This package provides a comprehensive localization system for the
 * UnifiedPlugin API, supporting multiple languages, per-player locale
 * preferences, and seamless integration with Adventure's MiniMessage.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Per-player locales:</b> Each player can have their own language preference</li>
 *   <li><b>Fallback chain:</b> Player locale -> Server default -> English</li>
 *   <li><b>MiniMessage integration:</b> Full support for Adventure text components</li>
 *   <li><b>Placeholder replacement:</b> Type-safe placeholder system</li>
 *   <li><b>Pluralization:</b> Automatic plural form selection</li>
 *   <li><b>Hot reload:</b> Update translations without server restart</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Get the i18n service
 * I18nService i18n = UnifiedAPI.services().get(I18nService.class);
 *
 * // Send a localized message to a player
 * i18n.sendMessage(player, MessageKey.of("welcome.message"),
 *     Replacement.of("player", player.getName()));
 *
 * // Get a formatted component
 * Component message = i18n.translate(locale, MessageKey.of("balance.display"),
 *     Replacement.of("amount", 1000));
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see sh.pcx.unified.i18n.I18nService
 * @see sh.pcx.unified.i18n.core.Locale
 * @see sh.pcx.unified.i18n.messages.MessageBundle
 */
package sh.pcx.unified.i18n;
