/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.server.common.infra.reflect;

import lombok.Data;
import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;

/**
 * @author James Chen
 */
@Data
public class FieldAndMethodHandledBasedVarAccessor<T, V> implements VarAccessor<T, V> {

    private final Field fieldForGetter;
    private final MethodHandle setter;

    public FieldAndMethodHandledBasedVarAccessor(Field fieldForGetter, MethodHandle setter) {
        ReflectionUtil.setAccessible(fieldForGetter);
        this.fieldForGetter = fieldForGetter;
        this.setter = setter;
    }

    @SneakyThrows
    @Override
    public V get(T object) {
        return (V) fieldForGetter.get(object);
    }

    @SneakyThrows
    @Override
    public void set(T object, V value) {
        setter.invoke(object, value);
    }

}
