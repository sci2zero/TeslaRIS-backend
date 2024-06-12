package rs.teslaris.core.converter;

import java.util.Collection;
import java.util.stream.Collectors;

public abstract class AbstractConverter<TFrom, TTo> implements IConverter<TFrom, TTo> {

    public Collection<TTo> convert(Collection<TFrom> source) {
        return source.stream().map(this::convert).collect(Collectors.toList());
    }

}
