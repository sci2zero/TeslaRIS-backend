package rs.teslaris.core.importer.utility;

public interface CommonRecordConverter<T, D> {

    D toImportDTO(T record);
}
