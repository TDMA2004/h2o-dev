package water.api;

import water.H2O;
import water.Iced;

/**
 * Summary page referencing all tutorials.
 *
 * @author michal
 */
class TutorialsHandler extends Handler<TutorialsHandler.Tutorials,TutorialsV1> {
  // Supported at V1 same as always
  @Override protected int min_ver() { return 1; }
  @Override protected int max_ver() { return Integer.MAX_VALUE; }

  public static final class Tutorials extends Iced {
    static final String HTML =
            "<div class='container'><div class='hero-unit' style='overflow: hidden'>"
                    + "<style scoped='scoped'>"
                    + "  h2 { font-size:18px; }"
                    + "  p { font-size:16px; }"
                    + "</style>"
                    + "<h1>H<sub>2</sub>O Tutorials</h1>"
                    + "<blockquote><small>A unique way to explore H<sub>2</sub>O</small></blockquote>"

                    + "</div>"
                    + "<div class='row'>"

                    + "<div class='span4 col'>"
                    + "  <h2>Steam</h2>"
                    + "  <a href='/steam/index.html'><img src='/img/steam.png' style='border:1px solid #ddd;max-width:100%;height:auto'></a>"
                    + "</div>"

                    + "<div class='span2 col'>"
                    + "  <h2>Random Forest</h2>"
                    + "<p>Random Forest is a classical machine learning algorithm for classification and regression. Learn how to use it with H<sub>2</sub>O.</it></p>"
                    + "<a href='/TutorialRFIris.html' class='btn btn-primary'>Try it!</a>"
                    + "</div>"

                    + "<div class='span2 col'>"
                    + "<h2>GLM</h2>"
                    + "<p>Generalized linear model is a generalization of linear regression. Experience its unique power on top of H<sub>2</sub>O.</p>"
                    + "<a href='/TutorialGLMProstate.html' class='btn btn-primary'>Try it!</a>"
                    + "</div>"

                    + "<div class='span2 col'>"
                    + "<h2>K-means</h2>"
                    + "<p>Perform cluster analysis with H<sub>2</sub>O. It employs K-means, a highly scalable clustering algorithm.</p>"
                    + "<a href='/TutorialKMeans.html' class='btn btn-primary'>Try it!</a>"
                    + "</div>"

                    + "<div class='span2 col'>"
                    + "<h2>Deep Learning</h2>"
                    + "<p>H<sub>2</sub>O's distributed Deep Learning models high-level abstractions in data with deep artificial neural networks.</p>"
                    + "<a href='/TutorialDeepLearning.html' class='btn btn-primary'>Try it!</a>"
                    + "</div>"

                    + "</div>"
                    + "</div>";
  }

  @Override protected void compute2() { throw H2O.fail(); }
  public TutorialsV1 nop(int version, TutorialsHandler.Tutorials ignoreme) { return schema(version); }
  // Tutorial Schemas are still at V1, unchanged for V2
  @Override protected TutorialsV1 schema(int version) { return new TutorialsV1(); }
}
