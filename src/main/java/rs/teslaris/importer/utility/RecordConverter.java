package rs.teslaris.importer.utility;

public interface RecordConverter<T, D> {
    D toDTO(T record);
}
