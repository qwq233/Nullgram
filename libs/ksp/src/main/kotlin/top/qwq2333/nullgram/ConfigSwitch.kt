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

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

class ConfigSwitchGenerator(
    private val codeGenerator: CodeGenerator, private val logger: KSPLogger
) : SymbolProcessor {
    private var finished = false
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val objectBuilder = TypeSpec.objectBuilder("Config")
        val configManager = ClassName("top.qwq2333.nullgram.config", "ConfigManager")
        val defines = ClassName("top.qwq2333.nullgram.utils", "Defines")
        val dependencies = mutableListOf<KSFile>()

        resolver.getSymbolsWithAnnotation("top.qwq2333.nullgram.BooleanConfig")
            .filterIsInstance<KSPropertyDeclaration>()
            .toList()
            .let {
                if (it.isEmpty()) return@let
                dependencies.addAll(it.map { it.containingFile!! })
                it.forEach { propertyDeclaration ->
                    val originName = propertyDeclaration.simpleName.getShortName()
                    val uppercaseName = originName[0].uppercaseChar() + originName.substring(1)
                    var defValue = false
                    propertyDeclaration.annotations.forEach { annotations ->
                        if (annotations.shortName.asString() == "BooleanConfig") {
                            annotations.arguments.forEach { argv ->
                                if (argv.name?.asString() == "defaultValue") {
                                    defValue = argv.value as Boolean
                                }
                            }
                        }
                    }

                    objectBuilder.apply {
                        addProperty(
                            PropertySpec.builder(originName, Boolean::class).apply {
                                mutable(true)
                                addAnnotation(JvmField::class)
                                initializer("%T.getBooleanOrDefault(%T.$originName, $defValue)", configManager, defines)
                            }.build()
                        )
                        addFunction(FunSpec.builder("set$uppercaseName").apply {
                            addAnnotation(JvmStatic::class)
                            addParameter("value", Boolean::class)
                            addStatement("%T.putBoolean(%T.$originName, value)", configManager, defines)
                        }.build())
                        addFunction(FunSpec.builder("toggle$uppercaseName").apply {
                            addStatement("$originName = !$originName")
                            addStatement("%T.putBoolean(%T.$originName, $originName)", configManager, defines)
                            addAnnotation(JvmStatic::class)
                        }.build())
                    }
                }
            }

        resolver.getSymbolsWithAnnotation("top.qwq2333.nullgram.IntConfig")
            .filterIsInstance<KSPropertyDeclaration>()
            .toList()
            .let {
                if (it.isEmpty()) return@let
                dependencies.addAll(it.map { it.containingFile!! })
                it.forEach { propertyDeclaration ->
                    val originName = propertyDeclaration.simpleName.getShortName()
                    val uppercaseName = originName[0].uppercaseChar() + originName.substring(1)
                    var defValue = 114514
                    propertyDeclaration.annotations.forEach { annotations ->
                        if (annotations.shortName.asString() == "IntConfig") {
                            annotations.arguments.forEach { argv ->
                                if (argv.name?.asString() == "defaultValue") {
                                    defValue = argv.value as Int
                                }
                            }
                        }
                    }

                    objectBuilder.apply {
                        addProperty(
                            PropertySpec.builder(originName, Int::class).apply {
                                mutable(true)
                                addAnnotation(JvmField::class)
                                initializer("%T.getIntOrDefault(%T.$originName, $defValue)", configManager, defines)
                            }.build()
                        )
                        addFunction(FunSpec.builder("set$uppercaseName").apply {
                            addParameter("value", Int::class)
                            addStatement("$originName = value")
                            addStatement("%T.putInt(%T.$originName, $originName)", configManager, defines)
                            addAnnotation(JvmStatic::class)
                        }.build())
                    }
                }
            }

        resolver.getSymbolsWithAnnotation("top.qwq2333.nullgram.StringConfig")
            .filterIsInstance<KSPropertyDeclaration>()
            .toList()
            .let {
                if (it.isEmpty()) return@let
                dependencies.addAll(it.map { it.containingFile!! })
                it.forEach { propertyDeclaration ->
                    val originName = propertyDeclaration.simpleName.getShortName()
                    val uppercaseName = originName[0].uppercaseChar() + originName.substring(1)
                    var defValue = "114514"
                    propertyDeclaration.annotations.forEach { annotations ->
                        if (annotations.shortName.asString() == "StringConfig") {
                            annotations.arguments.forEach { argv ->
                                if (argv.name?.asString() == "defaultValue") {
                                    defValue = argv.value as String
                                }
                            }
                        }
                    }

                    objectBuilder.apply {
                        addProperty(
                            PropertySpec.builder(originName, String::class).apply {
                                mutable(true)
                                addAnnotation(JvmField::class)
                                initializer("%T.getStringOrDefault($defines.$originName, %S)!!", configManager, defValue)
                            }.build()
                        )
                        addFunction(FunSpec.builder("set$uppercaseName").apply {
                            addParameter("value", String::class)
                            addStatement("$originName = value")
                            addStatement("%T.putString(%T.$originName, $originName)", configManager, defines)
                            addAnnotation(JvmStatic::class)
                        }.build())
                    }
                }
            }

        if (!finished) FileSpec.builder("top.qwq2333.gen", "Config")
            .addType(objectBuilder.build())
            .build()
            .writeTo(codeGenerator, Dependencies(true, *dependencies.toTypedArray()))
        finished = true

        return emptyList()
    }
}

class ConfigSwitchProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ConfigSwitchGenerator(environment.codeGenerator, environment.logger)
    }
}
