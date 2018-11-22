// Copyright (c) 2018 The Trapelo Group LLC
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package ttg.odata

package object client {

  /** Options with as much as possible being returned. */
  val DefaultDynamicsOptions = DynamicsOptions()

  /** Options with as little as possble being returned. */
  val QuietDynamicsOptions = DynamicsOptions(prefers = client.common.headers.QuietPreferOptions)

  /** Options with as little as possble being returned. */
  val NoisyDynamicsOptions = DynamicsOptions(prefers = client.common.headers.NoisyPreferOptions)

  /** All 0s GUID */
  val zeroGUID = "00000000-0000-0000-0000-000000000000"
}
