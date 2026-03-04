package rs.teslaris.core.model.document;

public interface PrintedPageable {

    Integer getNumberOfPages();

    void setNumberOfPages(Integer numberOfPages);

    String getStartPage();

    void setStartPage(String startPage);

    String getEndPage();

    void setEndPage(String endPage);

    String getArticleNumber();

    void setArticleNumber(String articleNumber);
}
