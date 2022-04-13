package codes.recursive.command;

import io.micronaut.core.annotation.Introspected;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Introspected
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
@Data
public class SearchCommand {
    private String searchString;
    private Integer offset = 0;
    private Integer max = 10;
}
