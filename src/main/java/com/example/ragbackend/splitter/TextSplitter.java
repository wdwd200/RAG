package com.example.ragbackend.splitter;

import java.util.List;

public interface TextSplitter {

    List<TextChunk> split(String text, SplitOptions options);
}
