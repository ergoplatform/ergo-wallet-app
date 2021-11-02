/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.ergoplatform.ios;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSMutableDictionary;
import org.robovm.apple.foundation.NSNumber;
import org.robovm.apple.foundation.NSObject;
import org.robovm.apple.foundation.NSString;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * IOS Preferences implementation, kindly borrowed from libGDX
 */
public class IOSPreferences {
    NSMutableDictionary<NSString, NSObject> nsDictionary;
    File file;

    public IOSPreferences(NSMutableDictionary<NSString, NSObject> nsDictionary, String filePath) {
        this.nsDictionary = nsDictionary;
        this.file = new File(filePath);
    }

    public IOSPreferences putBoolean(String key, boolean val) {
        nsDictionary.put(convertKey(key), NSNumber.valueOf(val));
        return this;
    }

    public IOSPreferences putInteger(String key, int val) {
        nsDictionary.put(convertKey(key), NSNumber.valueOf(val));
        return this;
    }

    public IOSPreferences putLong(String key, long val) {
        nsDictionary.put(convertKey(key), NSNumber.valueOf(val));
        return this;
    }

    public IOSPreferences putFloat(String key, float val) {
        nsDictionary.put(convertKey(key), NSNumber.valueOf(val));
        return this;
    }

    public IOSPreferences putString(String key, String val) {
        nsDictionary.put(convertKey(key), new NSString(val));
        return this;
    }

    public IOSPreferences put(Map<String, ?> vals) {
        Set<String> keySet = vals.keySet();
        for (String key : keySet) {
            Object value = vals.get(key);
            if (value instanceof String) {
                putString(key, (String) value);
            } else if (value instanceof Boolean) {
                putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                putInteger(key, (Integer) value);
            } else if (value instanceof Long) {
                putLong(key, (Long) value);
            } else if (value instanceof Float) {
                putFloat(key, (Float) value);
            }
        }
        return this;
    }

    public boolean getBoolean(String key) {
        NSNumber value = (NSNumber) nsDictionary.get(convertKey(key));
        if (value == null) return false;
        return value.booleanValue();
    }

    public int getInteger(String key) {
        NSNumber value = (NSNumber) nsDictionary.get(convertKey(key));
        if (value == null) return 0;
        return value.intValue();
    }

    public long getLong(String key) {
        NSNumber value = (NSNumber) nsDictionary.get(convertKey(key));
        if (value == null) return 0L;
        return value.longValue();
    }

    public float getFloat(String key) {
        NSNumber value = (NSNumber) nsDictionary.get(convertKey(key));
        if (value == null) return 0f;
        return value.floatValue();
    }

    public String getString(String key) {
        NSString value = (NSString) nsDictionary.get(convertKey(key));
        if (value == null) return "";
        return value.toString();
    }

    public boolean getBoolean(String key, boolean defValue) {
        if (!contains(key)) return defValue;
        return getBoolean(key);
    }

    public int getInteger(String key, int defValue) {
        if (!contains(key)) return defValue;
        return getInteger(key);
    }

    public long getLong(String key, long defValue) {
        if (!contains(key)) return defValue;
        return getLong(key);
    }

    public float getFloat(String key, float defValue) {
        if (!contains(key)) return defValue;
        return getFloat(key);
    }

    public String getString(String key, String defValue) {
        if (!contains(key)) return defValue;
        return getString(key);
    }

    public Map<String, ?> get() {
        Map<String, Object> map = new HashMap<String, Object>();
        for (NSString key : nsDictionary.keySet()) {
            NSObject value = nsDictionary.get(key);
            map.put(key.toString(), value.toString());
        }
        return map;
    }

    public boolean contains(String key) {
        return nsDictionary.containsKey(convertKey(key));
    }

    public void clear() {
        nsDictionary.clear();
    }

    public void remove(String key) {
        nsDictionary.remove(convertKey(key));
    }

    private NSString convertKey(String key) {
        return new NSString(key);
    }

    public void flush() {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        if (!nsDictionary.write(file, false)) {
            System.err.println("Failed to write NSDictionary to file " + file);
        }
        pool.close();
    }
}
