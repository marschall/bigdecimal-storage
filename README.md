BigDecimal Storage
==================

Efficient storage for BigDecimal in special cases.

 * a 128 bit integer can hold a scale of up to 8 (4 bits) and a 120 bit mantissa
   * commonly has a footprint of 32 bytes
 * a 96 bit integer can hold a scale of up to 8 (4 bits) and a 84 bit mantissa
   * commonly has a footprint of 24 bytes
 