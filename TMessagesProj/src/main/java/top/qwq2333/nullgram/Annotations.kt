/*
 * Copyright (C) 2019-2023 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this software.
 *  If not, see
 * <https://www.gnu.org/licenses/>
 */

package top.qwq2333.nullgram

/**
 * The field's Int getter and setter with this annotation will be generated in the `Config` class.
 * @param defaultValue The default value of the field.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class IntConfig(val defaultValue: Int)

/**
 * The field's Boolean getter and setter with this annotation will be generated in the `Config` class.
 * @param defaultValue The default value of the field.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class BooleanConfig(val defaultValue: Boolean = false)

/**
 * The field's String getter and setter with this annotation will be generated in the `Config` class.
 * @param defaultValue The default value of the field.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class StringConfig(val defaultValue: String)

/**
 * The field's Float getter and setter with this annotation will be generated in the `Config` class.
 * @param defaultValue The default value of the field.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class FloatConfig(val defaultValue: Float)
