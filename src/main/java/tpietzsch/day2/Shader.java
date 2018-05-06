package tpietzsch.day2;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import java.nio.FloatBuffer;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;

public class Shader
{
	private final String vpName;

	private final String fpName;

	private final ShaderProgram prog;

	public Shader( final GL2ES2 gl, final String vpName, final String fpName )
	{
		this( gl, vpName, fpName, tryGetContext() );
	}

	private static Class<?> tryGetContext()
	{
		final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		try
		{
			final Class< ? > klass = Shader.class.getClassLoader().loadClass( stackTrace[ 3 ].getClassName() );
			System.out.println( "klass = " + klass );
			return klass;
		}
		catch ( final ClassNotFoundException e )
		{
			throw new RuntimeException( e );
		}
	}

	public Shader( final GL2ES2 gl, final String vpName, final String fpName, final Class<?> context )
	{
		this.vpName = vpName;
		this.fpName = fpName;

		final ShaderCode vs = ShaderCode.create( gl, GL_VERTEX_SHADER, context, "", null, vpName, true );
		final ShaderCode fs = ShaderCode.create( gl, GL_FRAGMENT_SHADER, context, "", null, fpName, true );
		vs.defaultShaderCustomization( gl, true, false );
		fs.defaultShaderCustomization( gl, true, false );

		prog = new ShaderProgram();
		prog.add( vs );
		prog.add( fs );
		prog.link( gl, System.err );
		vs.destroy( gl );
		fs.destroy( gl );
	}

	public void use( final GL2ES2 gl )
	{
		gl.glUseProgram( prog.program() );
	}

	public void setUniform( final GL2ES2 gl, final String name, final boolean value )
	{
		gl.glProgramUniform1i( prog.program(), gl.glGetUniformLocation( prog.program(), name ), value ? 1 : 0 );
	}

	public void setUniform( final GL2ES2 gl, final String name, final int value )
	{
		gl.glProgramUniform1i( prog.program(), gl.glGetUniformLocation( prog.program(), name ), value );
	}

	public void setUniform( final GL2ES2 gl, final String name, final float value )
	{
		gl.glProgramUniform1f( prog.program(), gl.glGetUniformLocation( prog.program(), name ), value );
	}

	public void setUniform( final GL2ES2 gl, final String name, final float v0, final float v1 )
	{
		gl.glProgramUniform2f( prog.program(), gl.glGetUniformLocation( prog.program(), name ), v0, v1 );
	}

	public void setUniform( final GL2ES2 gl, final String name, final float v0, final float v1, final float v2 )
	{
		gl.glProgramUniform3f( prog.program(), gl.glGetUniformLocation( prog.program(), name ), v0, v1, v2 );
	}

	public void setUniform( final GL2ES2 gl, final String name, final float v0, final float v1, final float v2, final float v3 )
	{
		gl.glProgramUniform4f( prog.program(), gl.glGetUniformLocation( prog.program(), name ), v0, v1, v2, v3 );
	}

	public void setUniform( final GL2ES2 gl, final String name, final float[] v )
	{
		switch ( v.length )
		{
		case 1:
			setUniform( gl, name, v[ 0 ] );
			break;
		case 2:
			setUniform( gl, name, v[ 0 ], v[ 1 ] );
			break;
		case 3:
			setUniform( gl, name, v[ 0 ], v[ 1 ], v[ 2 ] );
			break;
		case 4:
			setUniform( gl, name, v[ 0 ], v[ 1 ], v[ 2 ], v[ 3 ] );
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	public void setUniform( final GL2ES2 gl, final String name, final Matrix4fc value )
	{
		final FloatBuffer fb = Buffers.newDirectFloatBuffer(16);
		value.get( fb );
		gl.glProgramUniformMatrix4fv( prog.program(), gl.glGetUniformLocation( prog.program(), name ), 1, false, fb );
	}

	public void setUniform( final GL2ES2 gl, final String name, final Vector3fc value )
	{
		gl.glProgramUniform3f( prog.program(), gl.glGetUniformLocation( prog.program(), name ), value.x(), value.y(), value.z() );
	}
}
