(ns catalog.taxonomy
  (:require [clojure.spec.alpha :as s]))

; https://codice.atlassian.net/wiki/spaces/DDF/pages/36995079/Attributes

(def tax
  [{:attr        :title
    :description "A name for the resource"
    :type        :string
    :constraints "< 1024 characters"
    :source      "Dublin Core (http://dublincore.org/documents/2012/06/14/dcmi-terms/?v=elements#elements-title)"}
   {:attr        :source-id
    :description "ID of the source where the Metacard is cataloged. While this cannot be moved or renamed for legacy reasons, it should be treated as non-mappable, since this field is overwritten by the system when federated results are retrieved."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :metadata-content-type
    :deprecated  true
    :description "Content type of the resource"
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :metadata-content-type-version
    :deprecated  true
    :description "Version of the metadata content type of the resource"
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :metadata-target-namespace
    :deprecated  true
    :description "Target namespace of the metadata"
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :metadata
    :description "Additional XML metadata describing the resource"
    :type        :xml}
   {:attr        :location
    :description "The primary geospatial location of the resource"
    :type        :geometry
    :constraints "Valid Well Known Text (WKT)"
    :example     "POINT(150 30)"}
   {:attr        :expiration
    :description "The expiration date of the resource"
    :type        :date}
   {:attr        :effective
    :deprecated  true
    :description "The \"effective\" time of the event or resource represented by the metacard. Deprecated in favor of created and modified."
    :type        :date}
   {:attr        :point-of-contact
    :deprecated  true
    :description "The name of the point of contact for the resource. This is set internally to the user's subject and should be considered read-only to other DDF components."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :resource-uri
    :description "Location of the resource for the metacard"
    :type        :string
    :constraints "Valid URI per RFC 2396"}
   {:attr        :resource-download-url
    :description "URL location of the resource for the metacard. This attributes provides a resolvable URL to the download location of the resource."
    :type        :string
    :constraints "Valid URL per RFC 2396"}
   {:attr        :resource-size
    :description "Size in bytes of resource."
    :type        :string}
   {:attr        :thumbnail
    :description "The thumbnail for the resource in JPEG format."
    :type        :binary
    :constraints "<= 128 KB"}
   {:attr        :description
    :description "An account of the resource."
    :type        :string
    :source      "Dublin Core (http://dublincore.org/documents/dcmi-terms/#elements-description)"}
   {:attr        :checksum
    :description "Checksum value for the primary resource for the metacard"
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :checksum-algorithm
    :description "Algorithm used to calculate the checksum on the primary resource of the metacard"
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :created
    :description "The creation date of the resource"
    :type        :date
    :source      "Dublin Core (http://dublincore.org/documents/dcmi-terms/#terms-created)"}
   {:attr        :modified
    :description "The modification date of the resource"
    :type        :date
    :source      "Dublin Core (http://dublincore.org/documents/dcmi-terms/#terms-modified)"}
   {:attr        :language
    :description "The language of the resource."
    :type        :string
    :constraints "Alpha-3 language code per ISO_639-2"
    :source      "Dublin Core (http://dublincore.org/documents/2012/06/14/dcmi-terms/?v=elements#language)"}
   {:attr        :id
    :description "Unique identifier for the metacard"
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :resource-derived-uri
    :description "Location of the derived formats for the metacard resource."
    :type        :string
    :constraints "Valid URI per RFC 2396"}
   {:attr        :resource-derived-download-url
    :description "Download URL for accessing the derived formats for the metacard resource."
    :type        :string
    :constraints "Valid URL per RFC 2396"}
   {:attr        :datatype
    :description "The generic type of the resource."
    :type        :string
    :constraints "Collection, Dataset, Event, Image, Interactive Resource, Service, Software, Sound, Text, Video, Document"
    :source      "Based on Dublin Core (http://dublincore.org/documents/2012/06/14/dcmi-terms/?v=elements#terms-type) and extended to include other DDF supported types."}
   {:attr        :metacard.created
    :description "The creation date of the metacard"
    :type        :date}
   {:attr        :metacard.modified
    :description "The modified date of the metacard"
    :type        :date}
   {:attr        :metacard.owner
    :description "The email address of the metacard owner."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :metacard-tags
    :description "Collections of data that go together, used for filtering query results."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :version.id
    :description "Internal attribute identifier for which metacard this version is representing"
    :type        :string}
   {:attr        :version.edited-by
    :description "Internal attribute identifying the editor of a history metacard"
    :type        :string}
   {:attr        :version.versioned-by
    :description "Internal attribute for the versioned date of a metacard version"
    :type        :date}
   {:attr        :version.action
    :description "Internal attribute for the action associated with a history metacard"
    :type        :string
    :constraints "One of \"Created\", \"Created-Content\", \"Updated\", \"Updated-Content\", \"Deleted\""}
   {:attr        :version.tags
    :description "Internal attribute for the tags that were on the original metacard"
    :type        :string}
   {:attr        :version.type
    :description "Internal attribute for the metacard type of the original metacard"
    :type        :string}
   {:attr        :version.type-binary
    :description "Internal attribute for the serialized metacard type of the original metacard"
    :type        :binary}
   {:attr        :associations.derived
    :description "ID of one or more metacards derived from this metacard."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :associations.related
    :description "ID of one or more metacards related to this metacard."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :associations.external
    :description "One or more URI's identifying external associated resources"
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :datetime.end
    :description "An end time for the resource"
    :type        :date}
   {:attr        :datetime.name
    :description "A descriptive name for the corresponding temporal attributes. See datetime.start and datetime.end."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :datetime.start
    :description "A start time for the resource"
    :type        :date}
   {:attr        :contact.creator-name
    :description "The name of this metacard's creator."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.creator-address
    :description "The physical address of this metacard's creator."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.creator-email
    :description "The email address of this metacard's creator."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.creator-phone
    :description "The phone number of this metacard's creator."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.publisher-name
    :description "The name of this metacard's publisher."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.publisher-address
    :description "The physical address of this metacard's publisher."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.publisher-email
    :description "The email address of this metacard's publisher."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.publisher-phone
    :description "The phone number of this metacard's publisher."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.contributor-name
    :description "The name of a contributor to this metacard."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.contributor-address
    :description "The physical address of a contributor to this metacard."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.contributor-email
    :description "The email address of a contributor to this metacard."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.contributor-phone
    :description "The phone number of a contributor to this metacard."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.point-of-contact-name
    :description "The name of a point of contact for this metacard."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.point-of-contact-address
    :description "The physical address of a point of contact for this metacard."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.point-of-contact-email
    :description "The email address of a point of contact for this metacard."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :contact.point-of-contact-phone
    :description "The phone number of a point of contact for this metacard."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :location.altitude-meters
    :description "Altitude of the resource in meters"
    :type        :double
    :constraints "> 0"}
   {:attr        :location.country-code
    :description "One or more country codes associated with the resource"
    :type        :string
    :constraints "ISO_3166-1 alpha-3 codes"}
   {:attr        :location.crs-code
    :description "Coordinate reference system code of the resource"
    :type        :string
    :constraints "< 1024 characters"
    :example     "EPSG:4326"}
   {:attr        :location.crs-name
    :description "Coordinate reference system name of the resource"
    :type        :string
    :constraints "< 1024 characters"
    :example     "WGS 84"}
   {:attr        :media.format
    :description "The file format, physical medium, or dimensions of the resource."
    :type        :string
    :constraints "< 1024 characters"
    :example     "txt, docx, xml - typically the extension or a more complete name for such, note that this is not the mime type"
    :source      "Dublin Core (http://dublincore.org/documents/dcmi-terms/#terms-modifiedhttp://dublincore.org/documents/dcmi-terms/#elements-format)"}
   {:attr        :media.format-version
    :description "The file format version of the resource."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :media.bit-rate
    :description "The bit rate of the media, in bps."
    :type        :double}
   {:attr        :media.frame-rate
    :description "The frame rate of the video, in fps."
    :type        :double}
   {:attr        :media.frame-center
    :description "The center of the video frame."
    :type        :geometry
    :constraints "Valid Well Known Text (WKT)"}
   {:attr        :media.height-pixels
    :description "The height of the media resource in pixels."
    :type        :integer}
   {:attr        :media.width-pixels
    :description "The width of the media resource in pixels."
    :type        :integer}
   {:attr        :media.compression
    :description "The type of compression this media uses."
    :type        :string
    :constraints "One of the values defined for EXIF Compression tag."
    :source      "EXIF http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/EXIF.html#Compression STANAG 4559 NC, NM, C1, M1, I1, C3, M3, C4, M4, C5, M5, C8, M8"}
   {:attr        :media.bits-per-sample
    :description "The number of bits per image component."
    :type        :integer
    :source      "EXIF"}
   {:attr        :media.type
    :description "A two-part identifier for file formats and format content"
    :type        :string
    :constraints "a valid mime-type"
    :example     "application/json"
    :source      "RFC 2046"}
   {:attr        :media.encoding
    :description "The encoding format of the media."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :media.number-of-bands
    :description "The number of spectral bands in the media."
    :type        :integer}
   {:attr        :media.scanning-mode
    :description "Indicate if progressive or interlaced scans are being applied."
    :type        :string
    :constraints "PROGRESSIVE, INTERLACED"
    :source      "MPEG2"}
   {:attr        :media.duration
    :description "The duration in seconds of the resource"
    :type        :double}
   {:attr        :security.access-groups
    :description "Attribute name for storing groups to enforce access controls upon"
    :type        :string}
   {:attr        :security.access-individuals
    :description "Attribute name for storing the email addresses of users to enforce access controls upon"
    :type        :string
    :constraints "Valid email address"}
   {:attr        :topic.category
    :description "A category code from a given vocabulary."
    :type        :string
    :constraints "should be restricted to the values in a given vocabulary"}
   {:attr        :topic.keyword
    :description "One or more keywords describing the subject matter of the metacard or resource."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :topic.vocabulary
    :description "An identifier of a controlled vocabulary from which the topic category is derived."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :validation-warnings
    :description "Textual description of validation warnings on the resource"
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :validation-errors
    :description "Textual description of validation errors on the resource"
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :isr.frequency-hertz
    :description "A frequency observation, typically of radio waves."
    :type        :float
    :constraints "normalized to Hz"}
   {:attr        :isr.target-id
    :description "A target identifier."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :isr.target-category-code
    :description "A STANAG 3596 target category."
    :type        :string
    :constraints "One of the values enumerated in STANAG 3596"
    :source      "STANAG 3596"}
   {:attr        :isr.platform-id
    :description "A platform identifier."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :isr.original-source
    :description "A STANAG 4545 ISOURCE."
    :type        :string
    :constraints "ISOURCE per STANAG 4545"
    :source      "STANAG 4545"}
   {:attr        :isr.organizational-unit
    :description "An organizational unit."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :isr.niirs
    :description "The quality or detail level of an image."
    :type        :integer
    :constraints "1-9"
    :source      "National Imagery Interpretability Rating Scale"}
   {:attr        :isr.nato-reporting-code
    :description "A reporting code as defined in STANAG 4545."
    :type        :string
    :constraints "One of the values in STANAG 4545"
    :source      "STANAG 4545"}
   {:attr        :isr.mission-id
    :description "A mission identifier."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :isr.sensor-type
    :description "A STANAG 4545 sensor type."
    :type        :string
    :constraints "STANAG 4545 ACFTB TRE"
    :source      "STANAG 4545"}
   {:attr        :isr.sensor-id
    :description "A STANAG 4545 sensor identifier."
    :type        :string
    :constraints "STANAG 4545 ACFTB TRE"
    :source      "STANAG 4545"}
   {:attr        :isr.cloud-cover
    :description "An imagery cloud cover percentage."
    :type        :integer
    :constraints "0-100"}
   {:attr        :isr.category
    :description "A STANAG 4559 image or video category."
    :type        :string
    :source      "STANAG 4559"}
   {:attr        :isr.image-id
    :description "An ISR imagery identifier."}
   {:attr        :isr.comments
    :description "An ISR comment."}
   {:attr        :isr.jc3iedm-id
    :description "A command and control interoperability identifier."}
   {:attr        :isr.platform-name
    :description "An ISR platform name."}
   {:attr        :isr.exploitation-level
    :description "The degree of exploitation performed on the original data. A value of '0' means that the product is not exploited."
    :type        :integer
    :constraints "0..9"
    :source      "STANAG 4559"}
   {:attr        :isr.exploitation-auto-generated
    :description "A flag indicating if the exploitation was automatically generated"
    :type        :boolean
    :source      "STANAG 4559"}
   {:attr        :isr.exploitation-subjective-quality-code
    :description "A subjective ISR quality code."
    :type        :string
    :constraints "EXCELLENT, FAIR, GOOD, POOR"
    :source      "STANAG 4559"}
   {:attr        :isr.vmti-processed
    :description "Whether or not the video has been processed for moving target indicators."
    :type        :boolean
    :source      "STANAG 4559"}
   {:attr        :isr.report-serial-number
    :description "Based on the originators request serial number STANAG 3277"
    :type        :string
    :constraints "< 1024 characters"
    :source      "STANAG 4559/3227"}
   {:attr        :isr.report-type
    :description "The type of the report."
    :type        :string
    :constraints "[IQREP, ISRSPOTREP, MIEXREP, MTIEXREP, RECCEXREP, WLEXREP, PENTAGRAM, INTREP, INTSUM, HUMINTREP]"
    :source      "STANAG 4559"}
   {:attr        :isr.report-info-rating
    :description "The info rating of the report."
    :type        :string
    :constraints "< 1024 characters"
    :source      "STANAG 4559"}
   {:attr        :isr.report-priority
    :description "The priority of the report."
    :type        :string
    :constraints "ROUTINE, PRIORITY, IMMEDIATE, FLASH"
    :source      "STANAG 4559"}
   {:attr        :isr.report-situation-type
    :description "The intel situation type."
    :type        :string
    :constraints "GENERAL, MILITARY, LAND, MARITIME, AIR, SPACE, CI/SECURITY, OTHER"
    :source      "STANAG 4559"}
   {:attr        :isr.report-entity-type
    :description "The type of the entity in the report."
    :type        :string
    :constraints "PLACE, EVENT, BIOGRAPHICS, ORGANISATION, EQUIPMENT"
    :source      "STANAG 4559"}
   {:attr        :isr.report-entity-name
    :description "The name of the entity in the report."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :isr.report-entity-alias
    :description "The alias of the entity in the report."
    :type        :string
    :constraints "< 1024 characters"}
   {:attr        :isr.rfi-for-action
    :description "A nation, command, agency, organization or unit requested to provide a response."
    :type        :string
    :constraints "<= 50 characters"
    :source      "STANAG 4559, STANAG 2149 edition 6"}
   {:attr        :isr.rfi-for-information
    :description "A multi-valued attribute identifying nations, commands, agencies, organizations and units which may have an interest in the response"
    :type        :string
    :constraints "<= 200 characters"
    :source      "STANAG 4559, STANAG 2149 (edition 6)"}
   {:attr        :isr.rfi-serial-number
    :description "An attribute for a unique human readable string identifying the RFI instance."
    :type        :string
    :constraints "<= 30 characters"}
   {:attr        :isr.rfi-status
    :description "An attribute identifying the status of the RFI."
    :type        :string
    :constraints "APPROVED | INACTION | STOPPED | FULFILLED"
    :source      "STANAG 4559"}
   {:attr        :isr.rfi-workflow-status
    :description "An attribute identifying the workflow status of the RFI."
    :type        :string
    :constraints "NEW | ACCEPTED | DENIED | CANCELLED | COMPLETED"
    :source      "STANAG 4559"}
   {:attr        :isr.task-comments
    :description "An attribute identifying comments related to the task."
    :type        :string
    :constraints "<= 255 characters"
    :source      "STANAG 4559"}
   {:attr        :isr.task-status
    :description "An attribute identifying the status of the task."
    :type        :string
    :constraints "PLANNED | ACKNOWLEDGED | ONGOING | ACCOMPLISHED | INTERRUPTED | INFEASIBLE | CANCELLED"
    :source      "STANAG 4559"}
   {:attr        :isr.task-id
    :description "An attribute for the task identifier."
    :type        :string}
   {:attr        :isr.cbrn-operation-name
    :description "The Chemical, Biological, Radiological & Nuclear (CBRN) Exercise Identification or Operation Code Word."
    :type        :string
    :constraints "<= 56 characters"
    :source      "STANAG 4559"}
   {:attr        :isr.cbrn-incident-number
    :description "The Chemical, Biological, Radiological & Nuclear (CBRN) Incident Number typically based on the concatenation of ALFA1, ALFA2, ALFA3, and ALFA4. The concatenation format is : ALPHA1 + space + ALPHA2 + space + ALPHA3 + space + ALPHA4.
     As an example: 'CA 938JTF 231 C' where :
     ALPHA1='CA'
     ALPHA2='938JTF'
     ALPHA3='231'
     ALPHA4='C'"
    :type        :string
    :constraints "<= 26 characters"
    :source      "STANAG 4559"}
   {:attr        :isr.cbrn-type
    :description "Type of Chemical, Biological, Radiological & Nuclear (CBRN) event enumeration description."
    :type        :string
    :constraints "CHEMICAL | BIOLOGICAL | RADIOLOGICAL | NUCLEAR | NOT KNOWN"
    :source      "STANAG 4559"}
   {:attr        :isr.cbrn-category
    :description "The Chemical, Biological, Radiological & Nuclear (CBRN) report type or plot type."
    :type        :string
    :constraints "<= 100 characters"
    :source      "STANAG 4559"}
   {:attr        :isr.cbrn-substance
    :description "Description of Chemical, Biological, Radiological & Nuclear (CBRN) substance."
    :type        :string
    :constraints "<= 7 characters"
    :source      "STANAG 4559"}
   {:attr        :isr.cbrn-alarm-classification
    :description "Classification of a Chemical, Biological, Radiological & Nuclear (CBRN) sensor alarm"
    :type        :string
    :constraints "ABOVE THRESHOLD | BELOW THRESHOLD"
    :source      "STANAG 4559"}
   {:attr        :isr.tdl-activity
    :description "A number that together with the platform number defines the identity of a track."
    :type        :short
    :constraints "0 .. 127"
    :source      "STANAG 4559, STANAG 5516"}
   {:attr        :isr.tdl-message-number
    :description "The Link 16 J Series message number."
    :type        :string
    :constraints "J2.2 | J2.3 | J2.5 | J3.0 | J3.2 | J3.3 | J3.5 | J3.7 | J7.0 | J7.1 | J7.2 | J7.3 | J14.0 | J14.2"
    :source      "STANAG 4559"}
   {:attr        :isr.tdl-track-number
    :description "Link 16 J Series track number for the track found in the product. The track number shall be in the decoded 5-character format (e.g. EK627)."
    :type        :string
    :constraints "<= 10 characters"
    :source      "STANAG 4559, STANAG 5516"}
   {:attr        :isr.video-mism-level
    :description "The \"Motion Imagery Systems (Spatial and Temporal) Matrix\" (MISM) defines an ENGINEERING GUIDELINE for the simple identification of broad categories of Motion Imagery Systems. The intent of the MISM is to give user communities an easy to use, common shorthand reference language to describe the fundamental technical capabilities of NATO motion imagery systems."
    :type        :integer
    :constraints "0 - 12"
    :source      "STANAG 4559"}
   {:attr        :isr.dwell-location
    :description "The geospatial location of the dwell area."
    :type        :geometry}
   {:attr        :isr.target-report-count
    :description "The count of the target reports in the dwell."
    :type        :integer}
   {:attr        :isr.mti-job-id
    :description "A platform assigned number identifying the specific request or task to which thee dwell pertains."
    :type        :long}
   {:attr        :isr.tdl-platform-number
    :description "A number that together with the 'activity' number defines the identity of a track"
    :type        :short
    :constraints "0 .. 63"
    :source      "STANAG 4559"}
   {:attr        :isr.snow-cover
    :description "The existence of snow. TRUE if snow is present, FALSE otherwise."
    :type        :boolean}
   {:attr        :isr.snow-depth-min-centimeters
    :description "The minimum depth of snow measured in centimeters."
    :type        :integer}
   {:attr        :isr.snow-depth-max-centimeters
    :description "The maximum depth of snow measured in centimeters."
    :type        :integer}
   {:attr        :security.classification
    :description "The overall classification of the metadata and resource."
    :type        :string}
   {:attr        :security.releasability
    :description "Identifies the country, countries, or organizations to which classified information may be released."
    :type        :string
    :constraints "ISO_3166-1 alpha-3 codes or country/organization tetragraphs per CAPCO Register Annex A."}
   {:attr        :security.classification-system
    :description "This attribute indicates the national or multinational security system used to classify the resource and its metadata."
    :type        :string
    :constraints "ISO_3166-1 alpha-3 codes"}
   {:attr        :security.owner-producer
    :description "Attribute identifying the national government or international organization owner(s) and/or producer(s) of the information."
    :type        :string
    :constraints "ISO_3166-1 alpha-3 codes"}
   {:attr        :security.dissemination-controls
    :description "This attribute is used to identify the expansion or limitation on the distribution of the information."
    :type        :string}
   {:attr        :security.codewords
    :description "This attribute provide further restrictions to the information based on controlled markings."
    :type        :string}
   {:attr        :security.other-dissemination-controls
    :description "This attribute is used to identify the expansion or limitation on the distribution of the information outside of its organization or community."
    :type        :string}
   {:attr        :ext.metadata-originator-classification
    :description "Attribute name for accessing the metadata originator classification."
    :type        :string}
   {:attr        :ext.metadata-classification
    :description "Attribute name for accessing the metadata classification."
    :type        :string}
   {:attr        :ext.metadata-classification-system
    :description "Attribute name for accessing the security classification system for the metadata."
    :type        :string}
   {:attr        :ext.metadata-dissemination-controls
    :description "Attribute name for accessing the metadata dissemination controls."
    :type        :string}
   {:attr        :ext.metadata-releasability
    :description "Attribute name for accessing the metadata releasability."
    :type        :string}
   {:attr        :ext.resource-releasability
    :description "Attribute name for accessing the resource releasability."
    :type        :string}
   {:attr        :ext.resource-originator-classification
    :description "Attribute name for accessing the resource originator classification."
    :type        :string}
   {:attr        :ext.resource-dissemination-controls
    :description "Attribute name for accessing the resource dissemination controls."
    :type        :string}
   {:attr        :ext.resource-classification-system
    :description "Attribute name for accessing the security classification system for the resource."
    :type        :string}
   {:attr        :ext.resource-classification
    :description "Attribute name for accessing the resource classification."
    :type        :string}])

