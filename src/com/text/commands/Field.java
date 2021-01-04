package com.text.commands;

public class Field {

    private String name;
    private String value;

    public Field(String name, String value) {
        setName(name);
        setValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String field) {
        this.value = field;
    }

    public static Field serialize(String text) {
        String[] data = text.split(",");
        Field field = new Field(data[0], data[1]);
        return field;
    }

    public static String deserialize(Field field) {
        String text = field.getName() + "," + field.getValue();
        return text;
    }
}
