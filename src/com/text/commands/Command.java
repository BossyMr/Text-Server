package com.text.commands;

import java.util.ArrayList;
import java.util.List;

public class Command {

    private String name;
    private List<Field> fields = new ArrayList<Field>();

    public Command(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public Field getField(String name) {
        for(Field field : fields) {
            if(field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public static Command serialize(String text) {
        if(!text.startsWith("TEXT")) {
            return null;
        }
        text = text.substring(4);
        String[] data = text.split(";");
        Command command = new Command(data[0]);
        for(int i = 1; i < data.length - 1; i++) {
            command.addField(Field.serialize(data[i]));
        }
        return command;
    }

    public static String deserialize(Command command) {
        String text = "TEXT" + command.getName() + ";";
        for(int i = 0; i < command.getFields().size(); i++) {
            text += Field.deserialize(command.getFields().get(i)) + ";";
        }
        return text;
    }
}
