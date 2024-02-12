package rs.teslaris.core.harvester.utility;

public interface RecordConverter<T, D> {
    D toDTO(T record);
}
