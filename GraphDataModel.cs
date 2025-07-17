using System;
using System.Collections.Generic;
using System.IO;
using System.Text.RegularExpressions;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.ComponentModel.DataAnnotations;

using PortfolioBim.Model.AccessService;
using PortfolioBim.Model.UseCase;

namespace PortfolioBim.Model.GraphData
{
    public class GraphDataModel
    {
        private string graphTemplate;
        private string graphData;
        private List<MetaDataNode> graphMetadata;
        private List<AccessRight> accessRights;
        private UseCase.UseCase useCase;

        /// <summary>
        /// Contains the serialized template of the graph. The file must have valid Turtle syntax.
        /// </summary>
        [Required]
        public string GraphTemplate
        {
            get => graphTemplate;
            set
            {
                if (IsValidTurtle(value))
                {
                    graphTemplate = value;
                }
                else
                {
                    throw new ArgumentException("The Turtle file contains invalid syntax.");
                }
            }
        }

        /// <summary>
        /// Serialized Turtle file with instances. The file must have valid Turtle syntax that matches the template graph.
        /// </summary>
        public string GraphData
        {
            get => graphData;
            set
            {
                if (IsValidTurtle(value))
                {
                    graphData = value;
                }
                else
                {
                    throw new ArgumentException("The Turtle file contains invalid syntax.");
                }
            }
        }

        /// <summary>
        /// List of access rights defined for the graph.
        /// </summary>
        public List<AccessRight> AccessRights
        {
            get => accessRights;
            set
            {
                accessRights = value;
            }
        }

        /// <summary>
        /// Contains the valid use case for this exchange scenario.
        /// </summary>
        public UseCase.UseCase UseCase
        {
            get => useCase;
            set
            {
                useCase = value;
            }
        }

        /// <summary>
        /// List of metadata nodes. Each node contains a unique ID, the class type, and a list of key-value pairs.
        /// </summary>
        [Required]
        public List<MetaDataNode> GraphMetadata
        {
            get => graphMetadata;
            set
            {
                if (AreValidMetadataEntries(value))
                {
                    graphMetadata = value;
                }
                else
                {
                    throw new ArgumentException("At least one metadata entry is invalid. All keys and values must be valid strings.");
                }
            }
        }

        public GraphDataModel()
        {
            // Initialization of the list of metadata.
            graphMetadata = new List<MetaDataNode>();
        }

        /// <summary>
        /// Serializes the current GraphDataModel and writes it to a file.
        /// </summary>
        /// <param name="filePath">The path of the file to write to.</param>
        public void SerializeToFile(string filePath)
        {
            string json = JsonSerializer.Serialize(this, new JsonSerializerOptions
            {
                WriteIndented = true // For nicely formatted JSON
            });

            File.WriteAllText(filePath, json);
        }

        /// <summary>
        /// serializes the current GraphDataModel to json
        /// </summary>
        /// <returns>content as string</returns>
        public string SerializeToJson()
        {
            return JsonSerializer.Serialize(this, new JsonSerializerOptions
            {
                WriteIndented = true // For nicely formatted JSON
            });
        }

        /// <summary>
        /// deserializes the json to a GraphDataModel
        /// </summary>
        /// <param name="json">The json content holding a valid GraphDataModel</param>
        /// <returns>GraphDataModel based on json input</returns>
        public static GraphDataModel DeserializeFromJson(string json)
        {
            return JsonSerializer.Deserialize<GraphDataModel>(json);
        }

        /// <summary>
        /// Deserializes a GraphDataModel from a JSON file.
        /// </summary>
        /// <param name="filePath">The path to the file to load.</param>
        /// <returns>A GraphDataModel object created from the file.</returns>
        public static GraphDataModel DeserializeFromFile(string filePath)
        {
            if (!File.Exists(filePath))
            {
                throw new FileNotFoundException("The file was not found.", filePath);
            }

            string json = File.ReadAllText(filePath);
            return JsonSerializer.Deserialize<GraphDataModel>(json);
        }

        /// <summary>
        /// Validates whether the given Turtle file has valid syntax.
        /// This function uses a simple regex as an example. A detailed validation would be more specific.
        /// </summary>
        /// <param name="turtleContent">The content of the Turtle file.</param>
        /// <returns>True if the Turtle syntax is valid, otherwise False.</returns>
        private static bool IsValidTurtle(string turtleContent)
        {
            if (string.IsNullOrWhiteSpace(turtleContent))
            {
                return false;
            }

            // Simple example check for Turtle syntax (expandable)
            string turtlePattern = @"@prefix\s+\w+:\s+<.*?>\s*\.";
            return Regex.IsMatch(turtleContent, turtlePattern);
        }

        /// <summary>
        /// Validates the metadata by checking if all nodes contain valid data.
        /// </summary>
        /// <param name="metadataEntries">The list of metadata nodes.</param>
        /// <returns>True if all metadata entries are valid, otherwise False.</returns>
        private static bool AreValidMetadataEntries(List<MetaDataNode> metadataEntries)
        {
            if (metadataEntries == null || metadataEntries.Count == 0)
            {
                return false;
            }

            foreach (var node in metadataEntries)
            {
                if (string.IsNullOrWhiteSpace(node.Id) || string.IsNullOrWhiteSpace(node.ClassType))
                {
                    return false;
                }

                foreach (var kvp in node.PropertiesValues)
                {
                    if (string.IsNullOrWhiteSpace(kvp.Key) || string.IsNullOrWhiteSpace(kvp.Value))
                    {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    /// <summary>
    /// Represents a single metadata node with a unique ID, a class type, and a list of properties.
    /// </summary>
    public class MetaDataNode
    {
        /// <summary>
        /// ID for the metadata node. Must be unique.
        /// </summary>
        [Required]
        public string Id { get; set; }

        /// <summary>
        /// Class type for the metadata node. Must be a valid class from the guideline.
        /// </summary>
        [Required]
        public string ClassType { get; set; }

        /// <summary>
        /// Key Value pairs for the metadata for the class type used. Must match the guideline class type properties.
        /// </summary>
        [Required]
        public Dictionary<string, string> PropertiesValues { get; set; }

        public MetaDataNode()
        {
            Id = string.Empty;
            ClassType = string.Empty;
            PropertiesValues = new Dictionary<string, string>();
        }
    }
}
