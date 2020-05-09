package sourcetrail;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NameHierarchy {

    @JsonProperty("name_delimiter")
    public String nameDelimiter;

    @JsonProperty("name_elements")
    public List<NameElement> nameElements = new ArrayList<>();

    public NameHierarchy(String nameDelimiter) {
        this.nameDelimiter = nameDelimiter;
    }

    public String serialize() {
        ObjectMapper om = new ObjectMapper();
        try {
            return om.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void push(NameElement element) {
        nameElements.add(element);
    }

    public void pop() {
        if (!nameElements.isEmpty()) {
            nameElements.remove(nameElements.size() - 1);
        }
    }

    public Optional<NameElement> peek() {
        if (!nameElements.isEmpty()) {
            return Optional.of(nameElements.get(nameElements.size() - 1));
        }
        return Optional.empty();
    }

}
