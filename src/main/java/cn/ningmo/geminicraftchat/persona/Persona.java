package cn.ningmo.geminicraftchat.persona;

public class Persona {
    private String name;
    private String description;
    private String context;

    public Persona(String name, String description, String context) {
        this.name = name;
        this.description = description;
        this.context = context;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContext() {
        return context;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setContext(String context) {
        this.context = context;
    }
} 