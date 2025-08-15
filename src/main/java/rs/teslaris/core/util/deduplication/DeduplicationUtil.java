package rs.teslaris.core.util.deduplication;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.importer.model.common.DocumentImport;

@Component
public class DeduplicationUtil {

    private static final String DJL_MODEL = "sentence-transformers/all-MiniLM-L6-v2";
    private static final String DJL_PATH = "djl://ai.djl.huggingface.pytorch/" + DJL_MODEL;

    public static Double MIN_SIMILARITY_THRESHOLD = 0.95;

    private static Predictor<String, float[]> predictor;

    @Autowired
    public DeduplicationUtil() throws ModelNotFoundException, MalformedModelException, IOException {
        Criteria<String, float[]> criteria =
            Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls(DJL_PATH)
                .optEngine("PyTorch")
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .optProgress(new ProgressBar())
                .build();

        ZooModel<String, float[]> model = criteria.loadModel();
        DeduplicationUtil.predictor = model.newPredictor();
    }

    public static String flattenJson(String json) {
        var gson = new GsonBuilder().create();
        var map = gson.fromJson(json, HashMap.class);
        return map.toString();
    }

    public static double cosineSimilarity(INDArray vectorA, INDArray vectorB) {
        double dotProduct = vectorA.mul(vectorB).sumNumber().doubleValue();
        double magnitudeA = vectorA.norm2Number().doubleValue();
        double magnitudeB = vectorB.norm2Number().doubleValue();
        return dotProduct / (magnitudeA * magnitudeB);
    }

    public static INDArray getEmbedding(String text) throws TranslateException {
        return Nd4j.create(predictor.predict(text));
    }

    public static boolean isDuplicate(DocumentImport backup, INDArray newEmbedding) {
        if (Objects.isNull(backup) || Objects.isNull(newEmbedding) ||
            Objects.isNull(backup.getEmbedding())) {
            return false;
        }

        var oldEmbedding = Nd4j.create(backup.getEmbedding());
        var similarity = DeduplicationUtil.cosineSimilarity(newEmbedding, oldEmbedding);
        return similarity > DeduplicationUtil.MIN_SIMILARITY_THRESHOLD;
    }

    public static List<Double> toDoubleList(INDArray embedding) {
        return Arrays.stream(embedding.toDoubleVector()).boxed().collect(Collectors.toList());
    }
}
