package org.quantumbadger.redreader.io;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import org.quantumbadger.redreader.common.UnexpectedInternalStateException;
import org.quantumbadger.redreader.io.WritableObject;
import org.quantumbadger.redreader.io.WritableObject.CreationData;
import org.quantumbadger.redreader.io.WritableObject.WritableField;
import org.quantumbadger.redreader.io.WritableObject.WritableObjectKey;
import org.quantumbadger.redreader.io.WritableObject.WritableObjectTimestamp;
import org.quantumbadger.redreader.io.WritableObject.WritableObjectVersion;

public class RawObjectDB<K, E extends WritableObject<K>> extends SQLiteOpenHelper {
    private static final String FIELD_ID = "RawObjectDB_id";
    private static final String FIELD_TIMESTAMP = "RawObjectDB_timestamp";
    private static final String TABLE_NAME = "objects";
    private final Class<E> clazz;
    private final String[] fieldNames;
    private final Field[] fields;

    private static <E> int getDbVersion(Class<E> clazz2) {
        Field[] declaredFields = clazz2.getDeclaredFields();
        int length = declaredFields.length;
        int i = 0;
        while (i < length) {
            Field field = declaredFields[i];
            if (field.isAnnotationPresent(WritableObjectVersion.class)) {
                field.setAccessible(true);
                try {
                    return field.getInt(null);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else {
                i++;
            }
        }
        throw new UnexpectedInternalStateException("Writable object has no DB version");
    }

    public RawObjectDB(Context context, String dbFilename, Class<E> clazz2) {
        Field[] declaredFields;
        super(context.getApplicationContext(), dbFilename, null, getDbVersion(clazz2));
        this.clazz = clazz2;
        LinkedList<Field> fields2 = new LinkedList<>();
        for (Field field : clazz2.getDeclaredFields()) {
            if ((field.getModifiers() & 128) == 0 && !field.isAnnotationPresent(WritableObjectKey.class) && !field.isAnnotationPresent(WritableObjectTimestamp.class) && field.isAnnotationPresent(WritableField.class)) {
                field.setAccessible(true);
                fields2.add(field);
            }
        }
        this.fields = (Field[]) fields2.toArray(new Field[fields2.size()]);
        this.fieldNames = new String[(this.fields.length + 2)];
        int i = 0;
        while (true) {
            Field[] fieldArr = this.fields;
            if (i < fieldArr.length) {
                this.fieldNames[i] = fieldArr[i].getName();
                i++;
            } else {
                String[] strArr = this.fieldNames;
                strArr[fieldArr.length] = FIELD_ID;
                strArr[fieldArr.length + 1] = FIELD_TIMESTAMP;
                return;
            }
        }
    }

    private String getFieldTypeString(Class<?> fieldType) {
        if (fieldType == Integer.class || fieldType == Long.class || fieldType == Integer.TYPE || fieldType == Long.TYPE) {
            return " INTEGER";
        }
        if (fieldType == Boolean.class || fieldType == Boolean.TYPE) {
            return " INTEGER";
        }
        return " TEXT";
    }

    public void onCreate(SQLiteDatabase db) {
        Field[] fieldArr;
        StringBuilder query = new StringBuilder("CREATE TABLE ");
        query.append(TABLE_NAME);
        query.append('(');
        query.append(FIELD_ID);
        query.append(" TEXT PRIMARY KEY ON CONFLICT REPLACE,");
        query.append(FIELD_TIMESTAMP);
        query.append(" INTEGER");
        for (Field field : this.fields) {
            query.append(',');
            query.append(field.getName());
            query.append(getFieldTypeString(field.getType()));
        }
        query.append(')');
        Log.i("RawObjectDB query string", query.toString());
        db.execSQL(query.toString());
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public synchronized Collection<E> getAll() {
        Cursor cursor;
        LinkedList<E> result;
        SQLiteDatabase db = getReadableDatabase();
        try {
            cursor = db.query(TABLE_NAME, this.fieldNames, null, null, null, null, null);
            result = new LinkedList<>();
            while (cursor.moveToNext()) {
                result.add(readFromCursor(cursor));
            }
            cursor.close();
            db.close();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e2) {
            throw new RuntimeException(e2);
        } catch (InvocationTargetException e3) {
            throw new RuntimeException(e3);
        } catch (Throwable th) {
            db.close();
            throw th;
        }
        return result;
    }

    public synchronized E getById(K id) {
        ArrayList<E> queryResult = getByField(FIELD_ID, id.toString());
        if (queryResult.size() != 1) {
            return null;
        }
        return (WritableObject) queryResult.get(0);
    }

    public synchronized ArrayList<E> getByField(String field, String value) {
        Cursor cursor;
        ArrayList<E> result;
        SQLiteDatabase db = getReadableDatabase();
        String str = TABLE_NAME;
        try {
            cursor = db.query(str, this.fieldNames, String.format(Locale.US, "%s=?", new Object[]{field}), new String[]{value}, null, null, null);
            result = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                result.add(readFromCursor(cursor));
            }
            cursor.close();
            db.close();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e2) {
            throw new RuntimeException(e2);
        } catch (InvocationTargetException e3) {
            throw new RuntimeException(e3);
        } catch (Throwable th) {
            db.close();
            throw th;
        }
        return result;
    }

    private E readFromCursor(Cursor cursor) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        try {
            WritableObject writableObject = (WritableObject) this.clazz.getConstructor(new Class[]{CreationData.class}).newInstance(new Object[]{new CreationData(cursor.getString(this.fields.length), cursor.getLong(this.fields.length + 1))});
            int i = 0;
            while (true) {
                Field[] fieldArr = this.fields;
                if (i >= fieldArr.length) {
                    return writableObject;
                }
                Field field = fieldArr[i];
                Class<?> fieldType = field.getType();
                Object obj = null;
                if (fieldType == String.class) {
                    if (!cursor.isNull(i)) {
                        obj = cursor.getString(i);
                    }
                    field.set(writableObject, obj);
                } else if (fieldType == Integer.class) {
                    if (!cursor.isNull(i)) {
                        obj = Integer.valueOf(cursor.getInt(i));
                    }
                    field.set(writableObject, obj);
                } else if (fieldType == Integer.TYPE) {
                    field.setInt(writableObject, cursor.getInt(i));
                } else if (fieldType == Long.class) {
                    if (!cursor.isNull(i)) {
                        obj = Long.valueOf(cursor.getLong(i));
                    }
                    field.set(writableObject, obj);
                } else if (fieldType == Long.TYPE) {
                    field.setLong(writableObject, cursor.getLong(i));
                } else if (fieldType == Boolean.class) {
                    if (!cursor.isNull(i)) {
                        obj = Boolean.valueOf(cursor.getInt(i) != 0);
                    }
                    field.set(writableObject, obj);
                } else if (fieldType == Boolean.TYPE) {
                    field.setBoolean(writableObject, cursor.getInt(i) != 0);
                } else if (fieldType == WritableHashSet.class) {
                    if (!cursor.isNull(i)) {
                        obj = WritableHashSet.unserializeWithMetadata(cursor.getString(i));
                    }
                    field.set(writableObject, obj);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid readFromCursor field type ");
                    sb.append(fieldType.getClass().getCanonicalName());
                    throw new UnexpectedInternalStateException(sb.toString());
                }
                i++;
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void put(E object) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            if (db.insertOrThrow(TABLE_NAME, null, toContentValues(object, new ContentValues(this.fields.length + 1))) >= 0) {
                db.close();
            } else {
                throw new RuntimeException("Database write failed");
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (Throwable th) {
            db.close();
            throw th;
        }
    }

    public synchronized void putAll(Collection<E> objects) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues values = new ContentValues(this.fields.length + 1);
            for (E object : objects) {
                if (db.insertOrThrow(TABLE_NAME, null, toContentValues(object, values)) < 0) {
                    throw new RuntimeException("Bulk database write failed");
                }
            }
            db.close();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (Throwable th) {
            db.close();
            throw th;
        }
    }

    private ContentValues toContentValues(E obj, ContentValues result) throws IllegalAccessException {
        result.put(FIELD_ID, obj.getKey().toString());
        result.put(FIELD_TIMESTAMP, Long.valueOf(obj.getTimestamp()));
        int i = 0;
        while (true) {
            Field[] fieldArr = this.fields;
            if (i >= fieldArr.length) {
                return result;
            }
            Field field = fieldArr[i];
            Class<?> fieldType = field.getType();
            if (fieldType == String.class) {
                result.put(this.fieldNames[i], (String) field.get(obj));
            } else if (fieldType == Integer.class) {
                result.put(this.fieldNames[i], (Integer) field.get(obj));
            } else if (fieldType == Integer.TYPE) {
                result.put(this.fieldNames[i], Integer.valueOf(field.getInt(obj)));
            } else if (fieldType == Long.class) {
                result.put(this.fieldNames[i], (Long) field.get(obj));
            } else if (fieldType == Long.TYPE) {
                result.put(this.fieldNames[i], Long.valueOf(field.getLong(obj)));
            } else if (fieldType == Boolean.class) {
                Boolean val = (Boolean) field.get(obj);
                result.put(this.fieldNames[i], val == null ? null : Integer.valueOf(val.booleanValue() ? 1 : 0));
            } else if (fieldType == Boolean.TYPE) {
                result.put(this.fieldNames[i], Integer.valueOf(field.getBoolean(obj) ? 1 : 0));
            } else if (fieldType == WritableHashSet.class) {
                result.put(this.fieldNames[i], ((WritableHashSet) field.get(obj)).serializeWithMetadata());
            } else {
                throw new UnexpectedInternalStateException();
            }
            i++;
        }
    }
}
