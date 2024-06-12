package rs.teslaris.core.importer.utility;

public interface RecordConverter<T, D> {
    D toDTO(T record);
}
