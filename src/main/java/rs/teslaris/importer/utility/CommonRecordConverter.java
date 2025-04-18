package rs.teslaris.importer.utility;

public interface CommonRecordConverter<T, D> {

    D toImportDTO(T record);
}
