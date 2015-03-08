package com.github.jpbetz.subspace

import java.nio.FloatBuffer

object Matrix4x4 {
  def apply(column1: Vector4, column2: Vector4, column3: Vector4, column4: Vector4): Matrix4x4 = Matrix4x4(
    column1.x, column1.y, column1.z, column1.w,
    column2.x, column2.y, column2.z, column2.w,
    column3.x, column3.y, column3.z, column3.w,
    column4.x, column4.y, column4.z, column4.w
  )

  lazy val identity = Matrix4x4(
    1, 0, 0, 0,
    0, 1, 0, 0,
    0, 0, 1, 0,
    0, 0, 0, 1)

  /**
   * Build a perspective transformation matrix.
   * @param fovRad Field of view, in radians.
   * @param aspect Width divided by height
   */
  def forPerspective(fovRad: Float, aspect: Float, near: Float, far: Float): Matrix4x4 = {
    val fov = 1 / scala.math.tan(fovRad / 2f).toFloat
    Matrix4x4(
      fov / aspect, 0, 0, 0,
      0, fov, 0, 0,
      0, 0, (far + near) / (near - far), -1,
      0, 0, (2 * far * near) / (near - far), 0)
  }

  /**
   * Build an orthographic transformation matrix
   */
  def forOrtho(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4x4 = {
    val w = right - left
    val h = top - bottom
    val p = far - near

    val x = (right + left) / w
    val y = (top + bottom) / h
    val z = (far + near) / p

    Matrix4x4(
      2/w, 0,   0,    0,
      0,   2/h, 0,    0,
      0,   0,   -2/p, 0,
      -x,  -y,  -z,   1)
  }

  def forRotation(q: Quaternion) = {
    val Quaternion(x, y, z, w) = q
    val x2 = x + x
    val y2 = y + y
    val z2 = z + z

    val xx = x * x2
    val xy = x * y2
    val xz = x * z2

    val yy = y * y2
    val yz = y * z2
    val zz = z * z2

    val wx = w * x2
    val wy = w * y2
    val wz = w * z2

    Matrix4x4(
      1 - ( yy + zz ), xy + wz, xz - wy, 0,
      xy - wz, 1 - ( xx + zz ), yz + wx, 0,
      xz + wy, yz - wx, 1 - ( xx + yy ), 0,
      0, 0, 0, 1)
  }

  def forTranslation(translation: Vector3): Matrix4x4 = {
    // m[column][row]
    Matrix4x4(
      1, 0, 0, 0,
      0, 1, 0, 0,
      0, 0, 1, 0,
      translation.x, translation.y, translation.z, 1)
  }

  def forScale(scale: Vector3): Matrix4x4 = {
    Matrix4x4(
      scale.x, 0, 0, 0,
      0, scale.y, 0, 0,
      0, 0, scale.z, 0,
      0, 0, 0, 1)
  }

  /**
   * Translates, rotates and then scales.
   */
  def forTranslationRotationScale(translation: Vector3, rotation: Quaternion, scale: Vector3): Matrix4x4 = {
    forTranslation(translation) * forRotation(rotation) * forScale(scale)
  }
}

/**
 * Constructor takes values in column major order.  That is,  the first 4 values are the rows the first column.
 */
// Originally the matrix cells were put into fields in anticipation of making this all stack allocated (using AnyVal
// or similar).  That's not currently possible,  so this could potentiall be transitioned to a simple Float array.
case class Matrix4x4(
    m00: Float, m01: Float, m02: Float, m03: Float,
    m10: Float, m11: Float, m12: Float, m13: Float,
    m20: Float, m21: Float, m22: Float, m23: Float,
    m30: Float, m31: Float, m32: Float, m33: Float)
  extends Bufferable{

  def unary_- : Matrix4x4 = negate
  def negate: Matrix4x4 = {
    Matrix4x4(
      -m00, -m01, -m02, -m03,
      -m10, -m11, -m12, -m13,
      -m20, -m21, -m22, -m23,
      -m30, -m31, -m32, -m33)
  }

  def *(f: Float): Matrix4x4 = multiply(f)
  def multiply(f: Float): Matrix4x4 = {
    Matrix4x4(
      m00*f, m01*f, m02*f, m03*f,
      m10*f, m11*f, m12*f, m13*f,
      m20*f, m21*f, m22*f, m23*f,
      m30*f, m31*f, m32*f, m33*f)
  }

  def *(m: Matrix4x4): Matrix4x4 = multiply(m)
  def multiply(m: Matrix4x4): Matrix4x4 = {
    Matrix4x4(
      m00 * m.m00 + m10 * m.m01 + m20 * m.m02 + m30 * m.m03,  m01 * m.m00 + m11 * m.m01 + m21 * m.m02 + m31 * m.m03,  m02 * m.m00 + m12 * m.m01 + m22 * m.m02 + m32 * m.m03,  m03 * m.m00 + m13 * m.m01 + m23 * m.m02 + m33 * m.m03,
      m00 * m.m10 + m10 * m.m11 + m20 * m.m12 + m30 * m.m13,  m01 * m.m10 + m11 * m.m11 + m21 * m.m12 + m31 * m.m13,  m02 * m.m10 + m12 * m.m11 + m22 * m.m12 + m32 * m.m13,  m03 * m.m10 + m13 * m.m11 + m23 * m.m12 + m33 * m.m13,
      m00 * m.m20 + m10 * m.m21 + m20 * m.m22 + m30 * m.m23,  m01 * m.m20 + m11 * m.m21 + m21 * m.m22 + m31 * m.m23,  m02 * m.m20 + m12 * m.m21 + m22 * m.m22 + m32 * m.m23,  m03 * m.m20 + m13 * m.m21 + m23 * m.m22 + m33 * m.m23,
      m00 * m.m30 + m10 * m.m31 + m20 * m.m32 + m30 * m.m33,  m01 * m.m30 + m11 * m.m31 + m21 * m.m32 + m31 * m.m33,  m02 * m.m30 + m12 * m.m31 + m22 * m.m32 + m32 * m.m33,  m03 * m.m30 + m13 * m.m31 + m23 * m.m32 + m33 * m.m33)
  }

  /**
   * Computes the matrix product of this matrix with a column vector.
   */
  def *(vec: Vector4): Vector4 = multiply(vec)

  /**
   * Computes the matrix product of this matrix with a column vector.
   */
  def multiply(vec: Vector4): Vector4 = {
    Vector4(
      m00 * vec.x + m10 * vec.y + m20 * vec.z + m30 * vec.w,
      m01 * vec.x + m11 * vec.y + m21 * vec.z + m31 * vec.w,
      m02 * vec.x + m12 * vec.y + m22 * vec.z + m32 * vec.w,
      m03 * vec.x + m13 * vec.y + m23 * vec.z + m33 * vec.w)
  }

  def transpose: Matrix4x4 = {
    Matrix4x4(
      m00, m10, m20, m30,
      m01, m11, m21, m31,
      m02, m12, m22, m32,
      m03, m13, m23, m33
    )
  }

  def determinant: Float = {
    val a = m11 * m22 * m33 + m21 * m32 * m13 + m31 * m12 * m23 - m13 * m22 * m31 - m23 * m32 * m11 - m33 * m12 * m21
    val b = m01 * m22 * m33 + m21 * m32 * m03 + m31 * m02 * m23 - m03 * m22 * m31 - m23 * m32 * m01 - m33 * m02 * m21
    val c = m01 * m12 * m33 + m11 * m32 * m03 + m31 * m02 * m13 - m03 * m12 * m31 - m13 * m32 * m01 - m33 * m02 * m11
    val d = m01 * m12 * m23 + m11 * m22 * m03 + m21 * m02 * m13 - m03 * m12 * m21 - m13 * m22 * m01 - m23 * m02 * m11

    m00 * a - m10 * b + m20 * c - m30 * d
  }

  def inverse: Matrix4x4 = {
    // Compute inverse of a Matrix using minors, cofactors and adjugate
    // TODO: fix this.  the below approach is crazy
    val matrixOfCofactors = Matrix4x4(
      +(m11 * m22 * m33 + m21 * m32 * m13 + m31 * m12 * m23 - m13 * m22 * m31 - m23 * m32 * m11 - m33 * m12 * m21),
      -(m10 * m22 * m33 + m20 * m32 * m13 + m30 * m12 * m23 - m13 * m22 * m30 - m23 * m32 * m10 - m33 * m12 * m20),
      +(m10 * m21 * m33 + m20 * m31 * m13 + m30 * m11 * m23 - m13 * m21 * m30 - m23 * m31 * m10 - m33 * m11 * m20),
      -(m10 * m21 * m32 + m20 * m31 * m12 + m30 * m11 * m22 - m12 * m21 * m30 - m22 * m31 * m10 - m32 * m11 * m20),

      -(m01 * m22 * m33 + m21 * m32 * m03 + m31 * m02 * m23 - m03 * m22 * m31 - m23 * m32 * m01 - m33 * m02 * m21),
      +(m00 * m22 * m33 + m20 * m32 * m03 + m30 * m02 * m23 - m03 * m22 * m30 - m23 * m32 * m00 - m33 * m02 * m20),
      -(m00 * m21 * m33 + m20 * m31 * m03 + m30 * m01 * m23 - m03 * m21 * m30 - m23 * m31 * m00 - m33 * m01 * m20),
      +(m00 * m21 * m32 + m20 * m31 * m02 + m30 * m01 * m22 - m02 * m21 * m30 - m22 * m31 * m00 - m32 * m01 * m20),

      +(m01 * m12 * m33 + m11 * m32 * m03 + m31 * m02 * m13 - m03 * m12 * m31 - m13 * m32 * m01 - m33 * m02 * m11),
      -(m00 * m12 * m33 + m10 * m32 * m03 + m30 * m02 * m13 - m03 * m12 * m30 - m13 * m32 * m00 - m33 * m02 * m10),
      +(m00 * m11 * m33 + m10 * m31 * m03 + m30 * m01 * m13 - m03 * m11 * m30 - m13 * m31 * m00 - m33 * m01 * m10),
      -(m00 * m11 * m32 + m10 * m31 * m02 + m30 * m01 * m12 - m02 * m11 * m30 - m12 * m31 * m00 - m32 * m01 * m10),

      -(m01 * m12 * m23 + m11 * m22 * m03 + m21 * m02 * m13 - m03 * m12 * m21 - m13 * m22 * m01 - m23 * m02 * m11),
      +(m00 * m12 * m23 + m10 * m22 * m03 + m20 * m02 * m13 - m03 * m12 * m20 - m13 * m22 * m00 - m23 * m02 * m10),
      -(m00 * m11 * m23 + m10 * m21 * m03 + m20 * m01 * m13 - m03 * m11 * m20 - m13 * m21 * m00 - m23 * m01 * m10),
      +(m00 * m11 * m22 + m10 * m21 * m02 + m20 * m01 * m12 - m02 * m11 * m20 - m12 * m21 * m00 - m22 * m01 * m10)
    )
    // TODO: if determinant is 0, throw a meaningful exception
    matrixOfCofactors.transpose.multiply(1 / determinant)
  }

  def normalMatrix: Matrix4x4 = inverse.transpose

  def column(index: Int): Vector4 = {
    index match {
      case 0 => Vector4(m00, m01, m02, m03)
      case 1 => Vector4(m10, m11, m12, m13)
      case 2 => Vector4(m20, m21, m22, m23)
      case 3 => Vector4(m30, m31, m32, m33)
      case _ => throw new IllegalArgumentException("index is out of range.  Must be between 0 and 4")
    }
  }

  def row(index: Int): Vector4 = {
    index match {
      case 0 => Vector4(m00, m10, m20, m30)
      case 1 => Vector4(m01, m11, m21, m31)
      case 2 => Vector4(m02, m12, m22, m32)
      case 3 => Vector4(m03, m13, m23, m33)
      case _ => throw new IllegalArgumentException("index is out of range.  Must be between 0 and 4")
    }
  }

  def allocateBuffer: FloatBuffer = {
    val direct = Buffers.createFloatBuffer(16)
    direct
      .put(m00).put(m01).put(m02).put(m03)
      .put(m10).put(m11).put(m12).put(m13)
      .put(m20).put(m21).put(m22).put(m23)
      .put(m30).put(m31).put(m32).put(m33)
    direct.flip()
    direct
  }

  def updateBuffer(buffer: FloatBuffer): Unit = {
    buffer.clear()
    buffer
      .put(m00).put(m01).put(m02).put(m03)
      .put(m10).put(m11).put(m12).put(m13)
      .put(m20).put(m21).put(m22).put(m23)
      .put(m30).put(m31).put(m32).put(m33)
    buffer.flip()
  }

  override def toString: String = {
    s"""[[$m00, $m01, $m02, $m03]
       | [$m10, $m11, $m12, $m13]
       | [$m20, $m21, $m22, $m23]
       | [$m30, $m31, $m32, $m33]]
     """.stripMargin
  }
}
