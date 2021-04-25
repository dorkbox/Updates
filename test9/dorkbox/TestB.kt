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

// https://youtrack.jetbrains.com/issue/KT-35343
// for now, this is a bug with intellij. Doing this will suppress errors in intellij. This is not a problem for gradle
//@file:Suppress("JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE")

package dorkbox

import dorkbox.updates.Updates
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

fun main() {
    Updates.add(TestB::class.java, UUID.randomUUID().toString().replace("-", ""), "121.0")
    Thread.sleep(TimeUnit.MINUTES.toMillis(5))
}

class TestB {
    @Test
    fun uuid() {
//        Updates.DEBUG = true  // requires GradleUtils.allowKotlinInternalAccessForTests
        Updates.add(TestB::class.java, UUID.randomUUID().toString().replace("-", ""), "121.0")
        Thread.sleep(TimeUnit.SECONDS.toMillis(5))
    }
}
