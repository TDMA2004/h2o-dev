package hex.schemas;

import hex.glm.GLM;
import hex.glm.GLMModel.GLMParameters;
import water.api.ModelParametersSchema;
import water.fvec.Frame;
import water.util.PojoUtils;

/**
 * Created by tomasnykodym on 8/29/14.
 */
public class GLMV2 extends ModelBuilderSchema<GLM,GLMV2,GLMV2.GLMParametersV2> {

  public static final class GLMParametersV2 extends ModelParametersSchema<GLMParameters, GLMParametersV2> {
    // TODO: parameters are all wrong. . .
    public String[] fields() { return new String[] { "destination_key", "K", "max_iters", "normalize", "seed" }; }

    // Input fields
    public int K;
    public int max_iters;        // Max iterations
    public boolean normalize = true;
    public long seed;

    @Override public GLMParametersV2 fillFromImpl(GLMParameters parms) {
      super.fillFromImpl(parms);
      return this;
    }

    public GLMParameters createImpl() {
      GLMParameters impl = new GLMParameters();
      PojoUtils.copyProperties(impl, this, PojoUtils.FieldNaming.DEST_HAS_UNDERSCORES);
      return impl;
    }
  }

  //==========================
  // Custom adapters go here

  @Override public GLMParametersV2 createParametersSchema() { return new GLMParametersV2(); }

  // TODO: refactor ModelBuilder creation
  @Override public GLM createImpl() {
    if( parameters.K < 2 || parameters.K > 9999999 ) throw new IllegalArgumentException("2<= K && K < 10000000");
    if( parameters.max_iters < 0 || parameters.max_iters > 9999999 ) throw new IllegalArgumentException("1<= max_iters && max_iters < 10000000");
    if( parameters.max_iters==0 ) parameters.max_iters = 1000; // Default is 1000 max_iters
    if( parameters.seed == 0 ) parameters.seed = System.nanoTime();

    GLMParameters parms = parameters.createImpl();
    return new GLM(parms);
  }

  // Return a URL to invoke GLM on this Frame
  @Override protected String acceptsFrame( Frame fr ) { return "/v2/GLM?training_frame="+fr._key; }
}
