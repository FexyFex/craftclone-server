package game.world.generation

import game.blocks.BlockRegistry
import math.datatype.vec.IVec3
import math.datatype.vec.Vec3
import util.for3D
import java.util.LinkedList
import java.util.Random
import kotlin.math.absoluteValue


class GeneratedStructure(val blockReplacements: MutableMap<IVec3, BlockReplacement> = mutableMapOf()) {

    companion object {
        val testStructure1 = GeneratedStructure()
        init {
            val stone = BlockRegistry.getIdByName("Stone")!!
            val dirt = BlockRegistry.getIdByName("Dirt")!!
            val grass = BlockRegistry.getIdByName("Grass")!!
            val leaves = BlockRegistry.getIdByName("Leaves")!!
            val stoneSlab = BlockRegistry.getIdByName("Stone Slab")!!
            val log = BlockRegistry.getIdByName("Log")!!
            for3D(-6..6, 6..14, -6..6) { x, y, z ->
                if ((Vec3(x,y,z) - Vec3(0, 8, 0)).length <= 6)
                    testStructure1.blockReplacements[IVec3(x,y,z)] = BlockReplacement({ b->b==0.toShort()}, leaves)
            }
            for3D(-1..1, -2..7, 0..0) { x, y, z ->
                testStructure1.blockReplacements[IVec3(x,y,z)] = BlockReplacement({ b->b==0.toShort()}, log)
            }
            for (i in -2 .. 7) {
                testStructure1.blockReplacements[IVec3(0,i,-1)] = BlockReplacement({ b->b==0.toShort()}, log)
                testStructure1.blockReplacements[IVec3(0,i,1)] = BlockReplacement({ b->b==0.toShort()}, log)
            }
            for (i in -2 .. 1) {
                testStructure1.blockReplacements[IVec3(0, i, -2)] = BlockReplacement({ b -> b == 0.toShort() }, log)
                testStructure1.blockReplacements[IVec3(0, i, 2)] = BlockReplacement({ b -> b == 0.toShort() }, log)
                testStructure1.blockReplacements[IVec3(2, i, 0)] = BlockReplacement({ b -> b == 0.toShort() }, log)
                testStructure1.blockReplacements[IVec3(-2, i, 0)] = BlockReplacement({ b -> b == 0.toShort() }, log)
            }
            testStructure1.blockReplacements[IVec3(0,-1,0)] = BlockReplacement({ b->b==grass}, dirt)
        }

        fun generateCopperVein(seed: Long = 0): GeneratedStructure {
            val ore = BlockRegistry.getIdByName("Copper Ore") ?: 1
            val stone = BlockRegistry.getIdByName("Stone") ?: 1
            val rng = Random(seed)
            val oreVein = GeneratedStructure()
            repeat(rng.nextInt(2)+1) {
                for3D(rng.nextInt(2)..rng.nextInt(4)+1,
                    rng.nextInt(2)..rng.nextInt(4)+1,
                    rng.nextInt(2)..rng.nextInt(4)+1) { x,y,z ->
                    oreVein.blockReplacements[IVec3(x,y,z)] = BlockReplacement({ b -> b == stone }, ore)
                }
            }
            return oreVein
        }

        fun generateDungeon(seed: Long = 0): GeneratedStructure {
            val rng = Random(seed)

            val dungeon = GeneratedStructure()
            val cobble = BlockRegistry.getIdByName("Cobblestone") ?: 1
            val stone = BlockRegistry.getIdByName("Stone") ?: 1
            val dirt = BlockRegistry.getIdByName("Dirt") ?: 1
            val grass = BlockRegistry.getIdByName("Grass") ?: 1

            val replacableBlocks = arrayOf(stone, cobble, grass, dirt)
            val airBlockReplacements: MutableMap<IVec3, BlockReplacement> = mutableMapOf()

            fun corridor(pos: IVec3, dir: IVec3, length: Int) {
                repeat(length) {
                    val currentPos = pos + dir * it
                    for3D( if(dir.x == 0) -2..2 else 0..0,
                        if(dir.y == 0) -2..2 else 0..0,
                        if(dir.z == 0) -2..2 else 0..0) { x, y, z ->
                        val currentBlock = currentPos + IVec3(x,y,z)
                        if (x.absoluteValue < 2 && y. absoluteValue < 2 && z.absoluteValue < 2)
                            airBlockReplacements[currentBlock] = BlockReplacement({ b -> b in replacableBlocks || b == cobble }, 0.toShort())
                        else
                            dungeon.blockReplacements[currentBlock] = BlockReplacement({ b -> b in replacableBlocks }, cobble)
                    }
                }
            }


            val directions = arrayOf(
                IVec3(0,0,1), IVec3(0,0,-1),
                IVec3(0,1,0), IVec3(0,-1,0),
                IVec3(1,0,0), IVec3(-1,0,0)
            )

            var remainingCorridors = rng.nextInt(15) + 5
            val doorQueue = LinkedList<Pair<IVec3, IVec3>>()
            doorQueue.add(Pair(IVec3(0,0,0), IVec3(0,-1,0)))
            while(!doorQueue.isEmpty() && remainingCorridors > 0) {
                remainingCorridors --
                val currentDoor = doorQueue.pop()

                val length = rng.nextInt(20) + 7
                corridor(currentDoor.first, currentDoor.second, length)
                var doorAdded = false
                repeat(rng.nextInt(3)+1) {
                    val dir = directions[rng.nextInt(directions.size)]
                    if (dir != -currentDoor.second) {
                        doorQueue.add(Pair(currentDoor.first+currentDoor.second*(length-2) - dir, dir))
                        doorAdded = true
                    }
                }
                if (!doorAdded) {
                    doorQueue.add(currentDoor)
                }
            }

            dungeon.blockReplacements.putAll(airBlockReplacements)

            return dungeon
        }
    }
}
