package codes.recursive.command;

import io.micronaut.core.annotation.Introspected;
import lombok.*;

@Introspected
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
@Data
public class SearchCommand {
    private String searchString;
    private Integer offset = 0;
    private Integer max = 10;
}
