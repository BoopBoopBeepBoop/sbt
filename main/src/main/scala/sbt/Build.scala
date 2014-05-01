/* sbt -- Simple Build Tool
 * Copyright 2011 Mark Harrah
 */
package sbt

import java.io.File
import Keys.{ name, organization, thisProject }
import Def.{ ScopedKey, Setting }

// name is more like BuildDefinition, but that is too long
trait Build {
  def projectDefinitions(baseDirectory: File): Seq[Project] = projects
  def projects: Seq[Project] = ReflectUtilities.allVals[Project](this).values.toSeq
  // TODO: Should we grab the build core setting shere or in a plugin?
  def settings: Seq[Setting[_]] = Defaults.buildCore
  def buildLoaders: Seq[BuildLoader.Components] = Nil
  /**
   * Explicitly defines the root project.
   * If None, the root project is the first project in the build's root directory or just the first project if none are in the root directory.
   */
  def rootProject: Option[Project] = None
}
// TODO 0.14.0: decide if Plugin should be deprecated in favor of AutoPlugin
trait Plugin {
  @deprecated("Override projectSettings or buildSettings instead.", "0.12.0")
  def settings: Seq[Setting[_]] = Nil

  /** Settings to be appended to all projects in a build. */
  def projectSettings: Seq[Setting[_]] = Nil

  /** Settings to be appended at the build scope. */
  def buildSettings: Seq[Setting[_]] = Nil

  /** Settings to be appended at the global scope. */
  def globalSettings: Seq[Setting[_]] = Nil
}

object Build {
  val defaultEmpty: Build = new Build { override def projects = Nil }
  val default: Build = new Build { override def projectDefinitions(base: File) = defaultProject(defaultID(base), base) :: Nil }
  def defaultAggregated(id: String, aggregate: Seq[ProjectRef]): Build = new Build {
    override def projectDefinitions(base: File) = defaultAggregatedProject(id, base, aggregate) :: Nil
  }

  def defaultID(base: File, prefix: String = "default"): String = prefix + "-" + Hash.trimHashString(base.getAbsolutePath, 6)
  @deprecated("Explicitly specify the ID", "0.13.0")
  def defaultProject(base: File): Project = defaultProject(defaultID(base), base)
  def defaultProject(id: String, base: File): Project = Project(id, base).settings(
    // TODO - Can we move this somewhere else?  ordering of settings is causing this to get borked.
    // if the user has overridden the name, use the normal organization that is derived from the name.
    organization := {
      val overridden = thisProject.value.id == name.value
      organization.?.value match {
        case Some(o) if !overridden => o
        case _                      => "default"
      }
      //(thisProject, organization, name) { (p, o, n) => if(p.id == n) "default" else o }
    }
  )
  def defaultAggregatedProject(id: String, base: File, agg: Seq[ProjectRef]): Project =
    defaultProject(id, base).aggregate(agg: _*)

  @deprecated("Use Attributed.data", "0.13.0")
  def data[T](in: Seq[Attributed[T]]): Seq[T] = Attributed.data(in)
  def analyzed(in: Seq[Attributed[_]]): Seq[inc.Analysis] = in.flatMap { _.metadata.get(Keys.analysis) }
}
