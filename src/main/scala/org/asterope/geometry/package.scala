package org.asterope

/** Geometry transformations.
  * This package was originally part of Skyview (skyview.geometry), was rewritten to Scala as part of Asterope projects.
  * Transformers are highly optimized, it uses pre-allocated arrays and matrix transforms.
  *
  * This package provides coordinate system transformations. Most often it is just rotation, Bessel transforms are more complicated.
  *
  * Projections from celestial sphere to flat plane are also provided.
  *
  * All classes are integrated in WCS (World Coordinate System) which is usable on charts, FITS images and general spherical image processing.
  */
package object geometry {}
