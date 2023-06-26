package rs.teslaris.core.converter;

import java.util.Collection;
import org.springframework.core.convert.converter.Converter;

public interface IConverter<TFrom, TTo> extends Converter<TFrom, TTo> {
    Collection<TTo> convert(Collection<TFrom> source);
}
