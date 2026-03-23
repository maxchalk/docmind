package com.docmind.dto;

import java.util.List;

public class QueryResponse {
    private String answer;
    private List<Source> sources;
    private boolean stale;
    private String documentName;

    public QueryResponse() {}
    public QueryResponse(String answer, List<Source> sources, boolean stale, String documentName) {
        this.answer = answer;
        this.sources = sources;
        this.stale = stale;
        this.documentName = documentName;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }
    public boolean isStale() { return stale; }
    public void setStale(boolean stale) { this.stale = stale; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String answer;
        private List<Source> sources;
        private boolean stale;
        private String documentName;
        public Builder answer(String answer) { this.answer = answer; return this; }
        public Builder sources(List<Source> sources) { this.sources = sources; return this; }
        public Builder isStale(boolean stale) { this.stale = stale; return this; }
        public Builder documentName(String documentName) { this.documentName = documentName; return this; }
        public QueryResponse build() { return new QueryResponse(answer, sources, stale, documentName); }
    }

    public static class Source {
        private Integer chunkIndex;
        private Integer pageNumber;
        private String snippet;

        public Source() {}
        public Source(Integer chunkIndex, Integer pageNumber, String snippet) {
            this.chunkIndex = chunkIndex;
            this.pageNumber = pageNumber;
            this.snippet = snippet;
        }

        public Integer getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
        public Integer getPageNumber() { return pageNumber; }
        public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }

        public static SourceBuilder builder() { return new SourceBuilder(); }
        public static class SourceBuilder {
            private Integer chunkIndex;
            private Integer pageNumber;
            private String snippet;
            public SourceBuilder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
            public SourceBuilder pageNumber(Integer pageNumber) { this.pageNumber = pageNumber; return this; }
            public SourceBuilder snippet(String snippet) { this.snippet = snippet; return this; }
            public Source build() { return new Source(chunkIndex, pageNumber, snippet); }
        }
    }
}
