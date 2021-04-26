/*
 * Copyright 2021 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// awful hack in settings.grade to reflectively enable logging
//val LoggerFactory = Class.forName("org.slf4j.LoggerFactory")
//val OutputEventListenerBackedLoggerContext = Class.forName("org.gradle.internal.logging.slf4j.OutputEventListenerBackedLoggerContext")
//val OutputEventListener = Class.forName("org.gradle.internal.logging.events.OutputEventListener")
//val StandardOutputListener = Class.forName("org.gradle.api.logging.StandardOutputListener")
//val StreamBackedStandardOutputListener = Class.forName("org.gradle.internal.logging.text.StreamBackedStandardOutputListener")
//val StyledTextOutput = Class.forName("org.gradle.internal.logging.text.StyledTextOutput")
//val StreamingStyledTextOutput = Class.forName("org.gradle.internal.logging.text.StreamingStyledTextOutput")
//val StyledTextOutputBackedRenderer = Class.forName("org.gradle.internal.logging.console.StyledTextOutputBackedRenderer")
//
//val newStreamBackedStandardOutputListener = StreamBackedStandardOutputListener.getDeclaredConstructor(java.io.OutputStream::class.java)
//
//val newStreamingStyledTextOutput = StreamingStyledTextOutput.getDeclaredConstructor(StandardOutputListener)
//val newStyledTextOutputBackedRenderer = StyledTextOutputBackedRenderer.getDeclaredConstructor(StyledTextOutput)
//
//val gradleLoggerFactory = LoggerFactory.getDeclaredMethod("getILoggerFactory").invoke(null)
//OutputEventListenerBackedLoggerContext.getDeclaredMethod("setLevel", LogLevel::class.java).invoke(gradleLoggerFactory, LogLevel.DEBUG)
//
//val streamBackedStandardOutputListener = newStreamBackedStandardOutputListener.newInstance(System.out)
//val streamingStyledTextOutput = newStreamingStyledTextOutput.newInstance(streamBackedStandardOutputListener)
//val styledTextOutputBackedRenderer = newStyledTextOutputBackedRenderer.newInstance(streamingStyledTextOutput)
//OutputEventListenerBackedLoggerContext.getDeclaredMethod("setOutputEventListener", OutputEventListener).invoke(gradleLoggerFactory, styledTextOutputBackedRenderer)

